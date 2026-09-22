# Architecture

The package deliberately separates four concerns:

1. **QuPath measurement kernel** — pixel classifier evaluation and ROI geometry.
2. **Adaptive statistical controller** — ratio estimator, finite-population uncertainty, spatial coverage and stopping law.
3. **Nextflow orchestration** — one QuPath JVM per image, retries, resource declarations, fan-out/fan-in and provenance.
4. **Make verbs** — human intent (`measure`, `estimate`, later `verify`, `complete`, `recover`, `export`).

The GUI and Groovy scripting API must round-trip the same immutable `AdaptiveMeasurementConfig`. The GUI's **Copy as Script** button is therefore not documentation text; it is generated from the exact config object that would be executed.

## Promotion gate

`WholeTissueMeasurementEngine` currently fails loudly. This is intentional. The attached production Groovy analysis is preserved unchanged at `scripts/whole_tissue_reference_v1.groovy` and remains the validated exact reference. The Java kernel is promoted only after fixtures demonstrate exact numerical parity and after adaptive stopping has independent simulation coverage.

## Target estimand

For sampled chunks `i`, with positive area `Y_i` and classified area `X_i`, the whole-tissue burden estimate is

`theta_hat = sum(Y_i) / sum(X_i)`.

Do not use the unweighted average of per-chunk percentages when chunks have unequal tissue area.
