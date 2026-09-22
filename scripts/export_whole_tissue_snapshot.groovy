import groovy.json.JsonOutput

// Run after either the preserved reference or promoted EXACT implementation.
def result = getAnnotationObjects().find { it.getName() == 'WHOLE_TISSUE_RESULT' }
if (result == null)
    throw new IllegalStateException('WHOLE_TISSUE_RESULT not found; exhaustive run did not finalize')

def m = result.getMeasurementList()
def snapshot = [
    positive_area_um2: m.get('Whole Tissue AT8: Positive area µm^2'),
    negative_area_um2: m.get('Whole Tissue AT8: Negative area µm^2'),
    positive_percent: m.get('Whole Tissue AT8: Positive %'),
    chunks_completed: Math.round(m.get('Whole Tissue AT8: Chunks completed')),
    chunks_failed: Math.round(m.get('Whole Tissue AT8: Chunks failed')),
    total_chunks: Math.round(m.get('Whole Tissue AT8: Total chunks')),
    chunk_size_um: m.get('Whole Tissue AT8: Chunk size µm')
]

def out = new File(System.getenv('QUPATH_PARITY_OUT') ?: 'whole_tissue_snapshot.json')
out.text = JsonOutput.prettyPrint(JsonOutput.toJson(snapshot)) + System.lineSeparator()
println 'Wrote parity snapshot: ' + out.getAbsolutePath()
