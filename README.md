# QuPath Adaptive Whole-Tissue Measurement

**SANGHA Research Systems × [Cherie Stringer, PhD](https://github.com/StringerCA)**

A QuPath-facing reference implementation for **exact** or **statistically controlled adaptive** whole-tissue pixel-classifier measurement.

The project uses the same interaction model as mature QuPath extensions: a GUI for ordinary use, a builder-style Groovy API for reproducibility/headless use, and **Copy as Script** so GUI configuration round-trips into runnable code.

## Status

**Exact-kernel promotion branch.** The validated QuPath 0.5.1 whole-tissue AT8 script is preserved unchanged in `scripts/whole_tissue_reference_v1.groovy`. Its exhaustive measurement semantics have now been promoted behind `AdaptiveMeasurementWorkflow` for `EXACT` mode. `ADAPTIVE_STATIONARY` remains deliberately release-blocked until runtime numerical parity is demonstrated against the preserved reference on the same image/classifier/annotations.

## Surfaces

### GUI

`Extensions → Adaptive whole tissue → Measure / estimate...`

Configure classifier, tissue annotation prefix, chunk size, exact/adaptive mode, tolerances, spatial strata and seed. **Copy as Script** places the corresponding builder invocation on the clipboard.

### Groovy

```groovy
import org.sangha.qupath.adaptive.controller.AdaptiveMeasurementWorkflow
import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig

def result = AdaptiveMeasurementWorkflow.builder()
    .classifierName("AT8 Classifier")
    .annotationPrefix("WT")
    .chunkSizeMicrons(2000)
    .mode(AdaptiveMeasurementConfig.Mode.ADAPTIVE_STATIONARY)
    .batchSize(16)
    .absoluteTolerance(0.0025)
    .driftTolerance(0.001)
    .stableBatches(3)
    .minimumChunks(32)
    .spatialBins(4, 4)
    .seed(42L)
    .build()
    .run()
```

Convenience calls are also provided through `AdaptiveMeasurementScripts.exact(...)` and `.estimate(...)`.

### Nextflow + Make

```bash
make measure  PROJECT=/path/project.qpproj MANIFEST=samples.tsv
make estimate PROJECT=/path/project.qpproj MANIFEST=samples.tsv
```

Nextflow owns multiprocessing across images; each QuPath JVM retains QuPath's internal threading.

## Repository layout

- `scripts/whole_tissue_reference_v1.groovy` — preserved production reference.
- `src/.../model` — immutable shared config/result types.
- `src/.../controller` — builder workflow and measurement-engine seam.
- `src/.../scripting` — convenience API + config-to-Groovy generator.
- `src/.../ui` — QuPath dialog + Copy as Script.
- `workflow/` — Nextflow fan-out across images.
- `Makefile` — semantic verbs.

## Target QuPath version

The extension scaffold targets QuPath 0.6.0 via the current QuPath Gradle-plugin pattern. The preserved reference analysis identifies itself as QuPath 0.5.1 and remains the parity oracle until the port is validated.

## Scientific contract

`EXACT` and `ADAPTIVE_STATIONARY` are distinct typed modes. An adaptive result must never masquerade as exhaustive whole-tissue measurement. The adaptive estimator will use tissue-area weighting, finite-population uncertainty, randomized/spatially balanced sampling and an explicit stopping reason. It is not enabled until the exact-parity gate is green.

## Exact-parity gate

The promoted engine exposes positive area, negative area, positive %, completed/failed/total chunk counts and chunk size as typed parity observables. After running the preserved reference and promoted `EXACT` path on the same QuPath image, export `WHOLE_TISSUE_RESULT` with `scripts/export_whole_tissue_snapshot.groovy` and compare the JSON snapshots:

```bash
make parity REFERENCE_JSON=reference.json PROMOTED_JSON=promoted.json
```

The comparator uses strict integer equality for chunk counts and tight floating-point tolerances for scientific measurements. Runtime is intentionally excluded from parity. The release gate is: `ADAPTIVE_STATIONARY` stays disabled until this comparison passes on the validation fixture set.

## Attribution

Scientific/domain workflow and pathology validation: **[Cherie Stringer, PhD](https://github.com/StringerCA)**.

Adaptive estimation, systems architecture, Nextflow orchestration, and QuPath extension engineering: **SANGHA Research Systems**.
