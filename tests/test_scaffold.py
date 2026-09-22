from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def must(path, text=None):
    p = ROOT / path
    assert p.exists(), p
    if text is not None:
        assert text in p.read_text(), (p, text)

# Preserved oracle
must('scripts/whole_tissue_reference_v1.groovy', 'WHOLE-TISSUE AT8 ANALYSIS')
must('scripts/whole_tissue_reference_v1.groovy', 'WHOLE_TISSUE_RESULT')

# Unified public surfaces
must('src/main/java/org/sangha/qupath/adaptive/scripting/ScriptGenerator.java', 'round-trips GUI configuration')
must('src/main/java/org/sangha/qupath/adaptive/controller/AdaptiveMeasurementWorkflow.java', 'builder()')
must('src/main/java/org/sangha/qupath/adaptive/ui/AdaptiveMeasurementDialog.java', 'Copy as Script')

# Exact kernel promotion gate
engine = (ROOT / 'src/main/java/org/sangha/qupath/adaptive/controller/WholeTissueMeasurementEngine.java').read_text()
assert 'return runExact(config);' in engine
assert 'ADAPTIVE_STATIONARY is release-blocked until EXACT numerical parity is green' in engine
assert 'Whole Tissue AT8: Positive area µm^2' in engine
assert 'Whole Tissue AT8: Negative area µm^2' in engine
assert 'Whole Tissue AT8: Positive %' in engine
assert 'WHOLE_TISSUE_RESULT' in engine
assert 'hierarchy.removeObject(chunk, true)' in engine
assert 'failedChunkNumbers.add(chunkNumber)' in engine

# Result must expose parity observables, not only a percentage.
result = (ROOT / 'src/main/java/org/sangha/qupath/adaptive/model/AdaptiveMeasurementResult.java').read_text()
for field in ['failedChunks', 'positiveAreaMicrons2', 'negativeAreaMicrons2', 'positiveFraction']:
    assert field in result

must('workflow/main.nf', 'process QUPATH_IMAGE')
must('Makefile', 'estimate:')
must('tools/compare_exact_parity.py', 'PARITY PASS')
print('static contract checks: OK')
