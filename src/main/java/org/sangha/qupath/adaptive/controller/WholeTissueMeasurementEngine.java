package org.sangha.qupath.adaptive.controller;

import java.util.ArrayList;
import java.util.List;

import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig;
import org.sangha.qupath.adaptive.model.AdaptiveMeasurementResult;

import qupath.lib.gui.scripting.QPEx;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.RoiTools;
import qupath.lib.roi.ROIs;
import qupath.opencv.ml.pixel.PixelClassifierTools;

/**
 * Production measurement kernel.
 *
 * <p>The EXACT path is a direct semantic promotion of
 * {@code scripts/whole_tissue_reference_v1.groovy}: same tissue discovery,
 * same rectangular lattice/intersection construction, same classifier-area
 * measurements, same pooled ratio, same failure semantics, and the same
 * WHOLE_TISSUE_RESULT measurement keys.</p>
 *
 * <p>ADAPTIVE_STATIONARY intentionally remains unavailable until exact-parity
 * fixtures pass against the preserved Groovy reference. This prevents the new
 * scheduler/statistical layer from outrunning its scientific oracle.</p>
 */
final class WholeTissueMeasurementEngine {
    static final String RESULT_NAME = "WHOLE_TISSUE_RESULT";
    static final String TEMP_PREFIX = "TEMP_WT_CHUNK_";

    private WholeTissueMeasurementEngine() {}

    static AdaptiveMeasurementResult run(AdaptiveMeasurementConfig config) {
        var imageData = QPEx.getCurrentImageData();
        if (imageData == null)
            throw new IllegalStateException("No image is open in QuPath");

        if (config.mode() == AdaptiveMeasurementConfig.Mode.ADAPTIVE_STATIONARY) {
            throw new UnsupportedOperationException(
                    "ADAPTIVE_STATIONARY is release-blocked until EXACT numerical parity is green " +
                    "against scripts/whole_tissue_reference_v1.groovy");
        }

        return runExact(config);
    }

    private static AdaptiveMeasurementResult runExact(AdaptiveMeasurementConfig config) {
        var imageData = QPEx.getCurrentImageData();
        var hierarchy = imageData.getHierarchy();

        // Reference semantics: remove stale temporary chunks from prior interrupted runs.
        var staleChunks = hierarchy.getAnnotationObjects().stream()
                .filter(p -> p.getName() != null && p.getName().startsWith(TEMP_PREFIX))
                .toList();
        if (!staleChunks.isEmpty())
            hierarchy.removeObjects(staleChunks, true);

        // Reference semantics: only one canonical result holder may exist.
        var oldResults = hierarchy.getAnnotationObjects().stream()
                .filter(p -> RESULT_NAME.equals(p.getName()))
                .toList();
        if (!oldResults.isEmpty())
            hierarchy.removeObjects(oldResults, true);

        var tissueAnnotations = hierarchy.getAnnotationObjects().stream()
                .filter(p -> {
                    var name = p.getName();
                    return name != null
                            && name.startsWith(config.annotationPrefix())
                            && !name.startsWith("TEMP_");
                })
                .toList();

        if (tissueAnnotations.isEmpty())
            throw new IllegalStateException(
                    "No tissue annotations found with prefix: " + config.annotationPrefix());

        var server = imageData.getServer();
        var calibration = server.getPixelCalibration();
        double pixelWidth = calibration.getPixelWidthMicrons();
        double pixelHeight = calibration.getPixelHeightMicrons();
        if (Double.isNaN(pixelWidth) || Double.isNaN(pixelHeight))
            throw new IllegalStateException("Invalid image calibration");

        double chunkWidthPixels = config.chunkSizeMicrons() / pixelWidth;
        double chunkHeightPixels = config.chunkSizeMicrons() / pixelHeight;

        var classifier = QPEx.loadPixelClassifier(config.classifierName());
        var classifierServer = PixelClassifierTools.createPixelClassificationServer(imageData, classifier);
        var manager = PixelClassifierTools.createMeasurementManager(classifierServer);

        List<qupath.lib.roi.interfaces.ROI> chunkROIs = new ArrayList<>();
        for (var tissue : tissueAnnotations) {
            var tissueROI = tissue.getROI();
            double minX = tissueROI.getBoundsX();
            double minY = tissueROI.getBoundsY();
            double maxX = minX + tissueROI.getBoundsWidth();
            double maxY = minY + tissueROI.getBoundsHeight();
            var plane = tissueROI.getImagePlane();

            for (double y = minY; y < maxY; y += chunkHeightPixels) {
                for (double x = minX; x < maxX; x += chunkWidthPixels) {
                    double w = Math.min(chunkWidthPixels, maxX - x);
                    double h = Math.min(chunkHeightPixels, maxY - y);
                    var rectangle = ROIs.createRectangleROI(x, y, w, h, plane);
                    var intersection = RoiTools.combineROIs(
                            tissueROI,
                            rectangle,
                            RoiTools.CombineOp.INTERSECT);
                    if (intersection != null && !intersection.isEmpty() && intersection.getArea() > 0)
                        chunkROIs.add(intersection);
                }
            }
        }

        int totalChunks = chunkROIs.size();
        if (totalChunks == 0)
            throw new IllegalStateException("No tissue chunks created");

        double totalPositive = 0.0;
        double totalNegative = 0.0;
        int completed = 0;
        int failed = 0;
        List<Integer> failedChunkNumbers = new ArrayList<>();

        for (int i = 0; i < totalChunks; i++) {
            int chunkNumber = i + 1;
            PathObject chunk = PathObjects.createAnnotationObject(chunkROIs.get(i));
            chunk.setName(TEMP_PREFIX + chunkNumber);
            hierarchy.addObject(chunk);
            boolean succeeded = false;

            try {
                PixelClassifierTools.addMeasurements(
                        List.of(chunk),
                        manager,
                        config.classifierName());

                var measurements = chunk.getMeasurementList();
                double positive = measurements.get(
                        config.classifierName() + ": Positive area µm^2");
                double negative = measurements.get(
                        config.classifierName() + ": Negative area µm^2");

                if (!Double.isNaN(positive) && !Double.isNaN(negative)) {
                    totalPositive += positive;
                    totalNegative += negative;
                    completed++;
                    succeeded = true;
                } else {
                    failed++;
                    failedChunkNumbers.add(chunkNumber);
                }
            } catch (Exception e) {
                failed++;
                failedChunkNumbers.add(chunkNumber);
            }

            // Preserve the reference recovery contract: failed chunks remain visible.
            if (succeeded)
                hierarchy.removeObject(chunk, true);
        }

        double totalClassified = totalPositive + totalNegative;
        double positiveFraction = totalClassified > 0
                ? totalPositive / totalClassified
                : Double.NaN;

        boolean exact = failed == 0 && completed == totalChunks;
        if (exact) {
            createReferenceCompatibleResult(
                    tissueAnnotations.get(0),
                    totalPositive,
                    totalNegative,
                    positiveFraction * 100.0,
                    completed,
                    failed,
                    totalChunks,
                    config.chunkSizeMicrons());
            QPEx.fireHierarchyUpdate();
        }

        String imageName = QPEx.getCurrentImageName();
        String stopReason = exact
                ? "EXHAUSTIVE_COMPLETE"
                : "INCOMPLETE_FAILED_CHUNKS:" + failedChunkNumbers;

        return new AdaptiveMeasurementResult(
                imageName,
                AdaptiveMeasurementConfig.Mode.EXACT,
                completed,
                totalChunks,
                failed,
                totalPositive,
                totalNegative,
                positiveFraction,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                false,
                exact,
                stopReason);
    }

    private static void createReferenceCompatibleResult(
            PathObject firstTissueAnnotation,
            double totalPositive,
            double totalNegative,
            double positivePercent,
            int completed,
            int failed,
            int totalChunks,
            double chunkSizeMicrons) {

        var plane = firstTissueAnnotation.getROI().getImagePlane();
        var resultROI = ROIs.createRectangleROI(0, 0, 1, 1, plane);
        var resultObject = PathObjects.createAnnotationObject(resultROI);
        resultObject.setName(RESULT_NAME);
        QPEx.getCurrentHierarchy().addObject(resultObject);

        var m = resultObject.getMeasurementList();
        m.put("Whole Tissue AT8: Positive area µm^2", totalPositive);
        m.put("Whole Tissue AT8: Negative area µm^2", totalNegative);
        m.put("Whole Tissue AT8: Positive %", positivePercent);
        m.put("Whole Tissue AT8: Chunks completed", completed);
        m.put("Whole Tissue AT8: Chunks failed", failed);
        m.put("Whole Tissue AT8: Total chunks", totalChunks);
        m.put("Whole Tissue AT8: Chunk size µm", chunkSizeMicrons);
        m.close();
    }
}
