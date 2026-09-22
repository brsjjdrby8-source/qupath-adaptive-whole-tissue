package org.sangha.qupath.adaptive.model;

/**
 * Typed result returned by every measurement surface.
 *
 * EXACT results expose the same sufficient statistics as the preserved production
 * Groovy reference so numerical parity can be asserted directly.
 */
public record AdaptiveMeasurementResult(
        String imageName,
        AdaptiveMeasurementConfig.Mode mode,
        int sampledChunks,
        int totalChunks,
        int failedChunks,
        double positiveAreaMicrons2,
        double negativeAreaMicrons2,
        double positiveFraction,
        double ciLower,
        double ciUpper,
        double ciHalfWidth,
        boolean stationaryStop,
        boolean exact,
        String stopReason
) {
    public double positivePercent() {
        return positiveFraction * 100.0;
    }

    public double classifiedAreaMicrons2() {
        return positiveAreaMicrons2 + negativeAreaMicrons2;
    }
}
