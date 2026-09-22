import qupath.lib.objects.PathObjects

import qupath.lib.roi.RoiTools

import qupath.lib.roi.ROIs

import qupath.opencv.ml.pixel.PixelClassifierTools

//
============================================================

// WHOLE-TISSUE AT8 ANALYSIS — PRODUCTION VERSION

// QuPath 0.5.1

//

// - Uses saved "AT8 Classifier"

// - Preserves classifier input resolution

// - Divides WT tissue computationally into 2000 x 2000 µm
chunks

// - Processes every WT annotation in the CURRENT image

// - Pools Positive + Negative area across all successful
chunks

// - Creates persistent WHOLE_TISSUE_RESULT for later export

// - Reports failed chunk numbers explicitly

//

// IMPORTANT:

// Run on ONE IMAGE AT A TIME.

// Do NOT use "Run for project".

//
============================================================

def imageData = getCurrentImageData()

def hierarchy = imageData.getHierarchy()

def classifierName = "AT8 Classifier"

double chunkSizeMicrons = 2000.0

print ""

print "=========================================="

print "WHOLE-TISSUE AT8 ANALYSIS"

print "=========================================="

print "Image: " + getCurrentImageName()

//
------------------------------------------------------------

// CLEAN UP OLD TEMPORARY CHUNKS

//
------------------------------------------------------------

def oldTempChunks = getAnnotationObjects().findAll {

    it.getName() !=
null &&

   
it.getName().startsWith("TEMP_WT_CHUNK_")

}

if (!oldTempChunks.isEmpty()) {

    print
"Removing " +

         
oldTempChunks.size() +

          "
leftover temporary chunks..."

   
hierarchy.removeObjects(

        oldTempChunks,

        true

    )

}

//
------------------------------------------------------------

// REMOVE OLD RESULT OBJECT

//

// Prevents accidentally having two result rows if this
image

// is deliberately rerun.

//
------------------------------------------------------------

def oldResults = getAnnotationObjects().findAll {

    it.getName() ==
"WHOLE_TISSUE_RESULT"

}

if (!oldResults.isEmpty()) {

    print
"Removing old WHOLE_TISSUE_RESULT..."

   
hierarchy.removeObjects(

        oldResults,

        true

    )

}

//
------------------------------------------------------------

// FIND WT ANNOTATIONS

//

// Excludes result object and temporary chunks.

//
------------------------------------------------------------

def wtAnnotations = getAnnotationObjects().findAll {

    def name =
it.getName()

    name != null
&&

    name.startsWith("WT")
&&

   
!name.startsWith("TEMP_")

}

if (wtAnnotations.isEmpty()) {

    print "ERROR:
No WT annotations found."

    return

}

print "WT annotations: " +

     
wtAnnotations.collect {

          it.getName()

      }.join(",
")

//
------------------------------------------------------------

// IMAGE CALIBRATION

//
------------------------------------------------------------

def server = imageData.getServer()

def calibration = server.getPixelCalibration()

double pixelWidth =

    calibration.getPixelWidthMicrons()

double pixelHeight =

   
calibration.getPixelHeightMicrons()

if (

   
Double.isNaN(pixelWidth) ||

   
Double.isNaN(pixelHeight)

) {

    print "ERROR:
Invalid image calibration."

    return

}

double chunkWidthPixels =

    chunkSizeMicrons /
pixelWidth

double chunkHeightPixels =

    chunkSizeMicrons /
pixelHeight

print String.format(

    "Native image
pixel size: %.6f x %.6f um",

    pixelWidth,

    pixelHeight

)

print String.format(

   
"Computational chunk size: %.0f x %.0f um",

    chunkSizeMicrons,

    chunkSizeMicrons

)

//
------------------------------------------------------------

// LOAD CLASSIFIER

//
------------------------------------------------------------

def classifier =

   
loadPixelClassifier(classifierName)

def classifierResolution =

    classifier

        .getMetadata()

       
.getInputResolution()

print "Classifier resolution: " +

     
classifierResolution

def classifierServer =

   
PixelClassifierTools

       
.createPixelClassificationServer(

            imageData,

            classifier

        )

def manager =

   
PixelClassifierTools

       
.createMeasurementManager(

           
classifierServer

        )

//
------------------------------------------------------------

// BUILD COMPUTATIONAL CHUNK GRID

//

// Chunks are intersections with WT tissue.

// They are NOT sampling ROIs.

// Every part of every WT annotation is included.

//
------------------------------------------------------------

def chunkROIs = []

wtAnnotations.each { wt ->

    def tissueROI =
wt.getROI()

    double minX =

       
tissueROI.getBoundsX()

    double minY =

       
tissueROI.getBoundsY()

    double maxX =

        minX +

       
tissueROI.getBoundsWidth()

    double maxY =

        minY +

       
tissueROI.getBoundsHeight()

    def plane =

       
tissueROI.getImagePlane()

    for (

        double y =
minY;

        y < maxY;

        y +=
chunkHeightPixels

    ) {

        for (

            double x =
minX;

            x <
maxX;

            x +=
chunkWidthPixels

        ) {

            double w =

               
Math.min(

                   
chunkWidthPixels,

                   
maxX - x

                )

            double h =

               
Math.min(

                   
chunkHeightPixels,

                   
maxY - y

                )

            def
rectangle =

               
ROIs.createRectangleROI(

                    x,

                    y,

                    w,

                    h,

                   
plane

                )

            def
intersection =

               
RoiTools.combineROIs(

                   
tissueROI,

                   
rectangle,

                   
RoiTools.CombineOp.INTERSECT

                )

            if (

               
intersection != null &&

               
!intersection.isEmpty() &&

               
intersection.getArea() > 0

            ) {

               
chunkROIs.add(

                   
intersection

                )

            }

        }

    }

}

int totalChunks =

    chunkROIs.size()

if (totalChunks == 0) {

    print "ERROR:
No tissue chunks created."

    return

}

print ""

print "Tissue chunks to process: " +

      totalChunks

print "Starting classification..."

print ""

//
------------------------------------------------------------

// PROCESS CHUNKS

//
------------------------------------------------------------

double totalPositive = 0.0

double totalNegative = 0.0

int completed = 0

int failed = 0

def failedChunkNumbers = []

long analysisStart =

   
System.currentTimeMillis()

for (

    int i = 0;

    i <
totalChunks;

    i++

) {

    int chunkNumber =
i + 1

    def roi =

        chunkROIs[i]

    def chunk =

       
PathObjects.createAnnotationObject(

            roi

        )

    chunk.setName(

       
"TEMP_WT_CHUNK_" +

        chunkNumber

    )

   
hierarchy.addObject(

        chunk

    )

    long chunkStart =

       
System.currentTimeMillis()

    print
String.format(

        "Starting
chunk %d / %d...",

        chunkNumber,

        totalChunks

    )

    boolean
chunkSucceeded = false

    try {

       
PixelClassifierTools.addMeasurements(

            [chunk],

            manager,

           
classifierName

        )

        def
measurements =

           
chunk.getMeasurementList()

        double
positive =

           
measurements.get(

               
classifierName +

               
": Positive area µm^2"

            )

        double
negative =

           
measurements.get(

               
classifierName +

               
": Negative area µm^2"

            )

        if (

           
!Double.isNaN(positive) &&

           
!Double.isNaN(negative)

        ) {

           
totalPositive += positive

           
totalNegative += negative

           
completed++

           
chunkSucceeded = true

            long
chunkElapsed =

               
System.currentTimeMillis() -

               
chunkStart

            long
totalElapsed =

               
System.currentTimeMillis() -

               
analysisStart

            double
avgSeconds =

               
completed > 0 ?

               
(totalElapsed / 1000.0) /

               
completed :

                0.0

            int
remaining =

               
totalChunks -

               
chunkNumber

            double
etaMinutes =

               
(avgSeconds * remaining) /

                60.0

            print
String.format(

               
"Finished %d/%d | %.1f sec | ETA %.1f min",

               
chunkNumber,

               
totalChunks,

                chunkElapsed
/ 1000.0,

               
etaMinutes

            )

        } else {

            failed++

           
failedChunkNumbers.add(

               
chunkNumber

            )

            print
"WARNING: Invalid measurements in chunk " +

                 
chunkNumber

        }

    } catch (Exception
e) {

        failed++

       
failedChunkNumbers.add(

           
chunkNumber

        )

        print
"ERROR in chunk " +

             
chunkNumber +

              ":
" +

             
e.getMessage()

    }

    // Successful
chunks are deleted immediately.

    //

    // FAILED chunks
are intentionally LEFT in the hierarchy

    // so you can see
exactly where the failure occurred.

    if
(chunkSucceeded) {

       
hierarchy.removeObject(

            chunk,

            true

        )

    }

}

//
------------------------------------------------------------

// CALCULATE POOLED RESULT

//
------------------------------------------------------------

double totalClassified =

    totalPositive +

    totalNegative

double positivePercent =

    totalClassified
> 0 ?

        (totalPositive
/

        
totalClassified) *

         100.0 :

        Double.NaN

long totalElapsed =

   
System.currentTimeMillis() -

    analysisStart

print ""

print "=========================================="

print "WHOLE-TISSUE AT8 RESULT"

print "=========================================="

print "Image: " +

     
getCurrentImageName()

print "Chunks completed: " +

      completed +

      " / "
+

      totalChunks

print "Chunks failed: " +

      failed

if (!failedChunkNumbers.isEmpty()) {

    print "FAILED
CHUNK NUMBERS: " +

         
failedChunkNumbers.join(", ")

}

print String.format(

    "Total
Positive area: %.4f um^2",

    totalPositive

)

print String.format(

    "Total
Negative area: %.4f um^2",

    totalNegative

)

print String.format(

    "Whole-tissue
AT8 Positive %%: %.6f",

    positivePercent

)

print String.format(

    "Total
runtime: %.2f minutes",

    totalElapsed /
60000.0

)

//
------------------------------------------------------------

// ONLY CREATE FINAL RESULT IF EVERY CHUNK SUCCEEDED

//

// This is important: an incomplete run should NOT silently

// become an apparently valid final result.

//
------------------------------------------------------------

if (failed == 0 &&

    completed ==
totalChunks) {

    // Create tiny
result-holder annotation.

    // It is NOT part
of the tissue analysis.

    def resultROI =

       
ROIs.createRectangleROI(

            0,

            0,

            1,

            1,

           
wtAnnotations[0]

               
.getROI()

               
.getImagePlane()

        )

    def resultObject =

       
PathObjects.createAnnotationObject(

            resultROI

        )

   
resultObject.setName(

       
"WHOLE_TISSUE_RESULT"

    )

   
hierarchy.addObject(

        resultObject

    )

    def
resultMeasurements =

       
resultObject.getMeasurementList()

   
resultMeasurements.put(

        "Whole
Tissue AT8: Positive area µm^2",

        totalPositive

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Negative area µm^2",

        totalNegative

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Positive %",

       
positivePercent

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Chunks completed",

        completed

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Chunks failed",

        failed

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Total chunks",

        totalChunks

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Chunk size µm",

       
chunkSizeMicrons

    )

   
resultMeasurements.put(

        "Whole
Tissue AT8: Runtime min",

        totalElapsed /
60000.0

    )

   
resultMeasurements.close()

   
fireHierarchyUpdate()

    print ""

    print
"WHOLE_TISSUE_RESULT created."

    print "Result
is ready for QuPath export."

    print ""

    print "SAVE
THE PROJECT."

} else {

    print ""

    print
"=========================================="

    print "RESULT
NOT FINALIZED"

    print
"=========================================="

    print "One or
more chunks failed."

    print
"Successful measurements have been summed"

    print "in the
console, but NO WHOLE_TISSUE_RESULT"

    print "was
created because the analysis is incomplete."

    print ""

    print "Failed
chunk(s): " +

         
failedChunkNumbers.join(", ")

    print ""

    print "DO NOT
rerun the entire image yet."

    print "Use a
recovery script for the failed chunk(s)."

}

print ""

print "=========================================="

print "DONE."