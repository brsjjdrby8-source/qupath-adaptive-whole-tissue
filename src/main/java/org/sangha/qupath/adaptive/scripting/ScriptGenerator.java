package org.sangha.qupath.adaptive.scripting;

import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig;

/** Generates a runnable Groovy snippet that round-trips GUI configuration. */
public final class ScriptGenerator {
    private ScriptGenerator() {}

    public static String generate(AdaptiveMeasurementConfig c) {
        return """
                import org.sangha.qupath.adaptive.controller.AdaptiveMeasurementWorkflow
                import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig

                def result = AdaptiveMeasurementWorkflow.builder()
                    .classifierName(%s)
                    .annotationPrefix(%s)
                    .chunkSizeMicrons(%s)
                    .mode(AdaptiveMeasurementConfig.Mode.%s)
                    .batchSize(%d)
                    .confidenceLevel(%s)
                    .absoluteTolerance(%s)
                    .driftTolerance(%s)
                    .stableBatches(%d)
                    .minimumChunks(%d)
                    .spatialBins(%d, %d)
                    .seed(%dL)
                    .build()
                    .run()

                println result
                """.formatted(
                quote(c.classifierName()), quote(c.annotationPrefix()), Double.toString(c.chunkSizeMicrons()),
                c.mode().name(), c.batchSize(), Double.toString(c.confidenceLevel()),
                Double.toString(c.absoluteTolerance()), Double.toString(c.driftTolerance()),
                c.stableBatches(), c.minimumChunks(), c.spatialBinsX(), c.spatialBinsY(), c.seed());
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
