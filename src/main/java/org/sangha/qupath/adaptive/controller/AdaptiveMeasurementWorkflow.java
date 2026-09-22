package org.sangha.qupath.adaptive.controller;

import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig;
import org.sangha.qupath.adaptive.model.AdaptiveMeasurementResult;

/**
 * Public builder API. The measurement engine is deliberately isolated behind this class so that
 * GUI, Groovy scripts and headless orchestration all invoke the same implementation.
 */
public final class AdaptiveMeasurementWorkflow {
    private final AdaptiveMeasurementConfig config;

    private AdaptiveMeasurementWorkflow(AdaptiveMeasurementConfig config) {
        this.config = config;
    }

    public static Builder builder() { return new Builder(); }

    public AdaptiveMeasurementResult run() {
        return WholeTissueMeasurementEngine.run(config);
    }

    public AdaptiveMeasurementConfig config() { return config; }

    public static final class Builder {
        private final AdaptiveMeasurementConfig.Builder delegate = AdaptiveMeasurementConfig.builder();
        public Builder classifierName(String v) { delegate.classifierName(v); return this; }
        public Builder annotationPrefix(String v) { delegate.annotationPrefix(v); return this; }
        public Builder chunkSizeMicrons(double v) { delegate.chunkSizeMicrons(v); return this; }
        public Builder mode(AdaptiveMeasurementConfig.Mode v) { delegate.mode(v); return this; }
        public Builder batchSize(int v) { delegate.batchSize(v); return this; }
        public Builder confidenceLevel(double v) { delegate.confidenceLevel(v); return this; }
        public Builder absoluteTolerance(double v) { delegate.absoluteTolerance(v); return this; }
        public Builder driftTolerance(double v) { delegate.driftTolerance(v); return this; }
        public Builder stableBatches(int v) { delegate.stableBatches(v); return this; }
        public Builder minimumChunks(int v) { delegate.minimumChunks(v); return this; }
        public Builder spatialBins(int x, int y) { delegate.spatialBins(x, y); return this; }
        public Builder seed(long v) { delegate.seed(v); return this; }
        public AdaptiveMeasurementWorkflow build() { return new AdaptiveMeasurementWorkflow(delegate.build()); }
    }
}
