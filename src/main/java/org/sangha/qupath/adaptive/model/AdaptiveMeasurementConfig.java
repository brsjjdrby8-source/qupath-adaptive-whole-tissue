package org.sangha.qupath.adaptive.model;

import java.util.Objects;

/** Immutable configuration shared by GUI, scripting and headless workflows. */
public record AdaptiveMeasurementConfig(
        String classifierName,
        String annotationPrefix,
        double chunkSizeMicrons,
        Mode mode,
        int batchSize,
        double confidenceLevel,
        double absoluteTolerance,
        double driftTolerance,
        int stableBatches,
        int minimumChunks,
        int spatialBinsX,
        int spatialBinsY,
        long seed
) {
    public enum Mode { EXACT, ADAPTIVE_STATIONARY }

    public AdaptiveMeasurementConfig {
        Objects.requireNonNull(classifierName, "classifierName");
        Objects.requireNonNull(annotationPrefix, "annotationPrefix");
        Objects.requireNonNull(mode, "mode");
        if (classifierName.isBlank()) throw new IllegalArgumentException("classifierName must not be blank");
        if (annotationPrefix.isBlank()) throw new IllegalArgumentException("annotationPrefix must not be blank");
        if (!(chunkSizeMicrons > 0)) throw new IllegalArgumentException("chunkSizeMicrons must be > 0");
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be >= 1");
        if (!(confidenceLevel > 0 && confidenceLevel < 1)) throw new IllegalArgumentException("confidenceLevel must be in (0,1)");
        if (!(absoluteTolerance > 0)) throw new IllegalArgumentException("absoluteTolerance must be > 0");
        if (!(driftTolerance >= 0)) throw new IllegalArgumentException("driftTolerance must be >= 0");
        if (stableBatches < 1) throw new IllegalArgumentException("stableBatches must be >= 1");
        if (minimumChunks < 2) throw new IllegalArgumentException("minimumChunks must be >= 2");
        if (spatialBinsX < 1 || spatialBinsY < 1) throw new IllegalArgumentException("spatial bins must be >= 1");
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String classifierName = "AT8 Classifier";
        private String annotationPrefix = "WT";
        private double chunkSizeMicrons = 2000.0;
        private Mode mode = Mode.ADAPTIVE_STATIONARY;
        private int batchSize = 16;
        private double confidenceLevel = 0.95;
        private double absoluteTolerance = 0.0025;
        private double driftTolerance = 0.0010;
        private int stableBatches = 3;
        private int minimumChunks = 32;
        private int spatialBinsX = 4;
        private int spatialBinsY = 4;
        private long seed = 42L;

        public Builder classifierName(String v) { classifierName = v; return this; }
        public Builder annotationPrefix(String v) { annotationPrefix = v; return this; }
        public Builder chunkSizeMicrons(double v) { chunkSizeMicrons = v; return this; }
        public Builder mode(Mode v) { mode = v; return this; }
        public Builder batchSize(int v) { batchSize = v; return this; }
        public Builder confidenceLevel(double v) { confidenceLevel = v; return this; }
        public Builder absoluteTolerance(double v) { absoluteTolerance = v; return this; }
        public Builder driftTolerance(double v) { driftTolerance = v; return this; }
        public Builder stableBatches(int v) { stableBatches = v; return this; }
        public Builder minimumChunks(int v) { minimumChunks = v; return this; }
        public Builder spatialBins(int x, int y) { spatialBinsX = x; spatialBinsY = y; return this; }
        public Builder seed(long v) { seed = v; return this; }

        public AdaptiveMeasurementConfig build() {
            return new AdaptiveMeasurementConfig(classifierName, annotationPrefix, chunkSizeMicrons, mode,
                    batchSize, confidenceLevel, absoluteTolerance, driftTolerance, stableBatches,
                    minimumChunks, spatialBinsX, spatialBinsY, seed);
        }
    }
}
