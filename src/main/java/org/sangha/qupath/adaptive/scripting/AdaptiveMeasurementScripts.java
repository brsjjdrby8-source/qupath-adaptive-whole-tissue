package org.sangha.qupath.adaptive.scripting;

import org.sangha.qupath.adaptive.controller.AdaptiveMeasurementWorkflow;
import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig;
import org.sangha.qupath.adaptive.model.AdaptiveMeasurementResult;

/** Convenience API for QuPath Groovy scripts. */
public final class AdaptiveMeasurementScripts {
    private AdaptiveMeasurementScripts() {}

    public static AdaptiveMeasurementResult exact(String classifierName) {
        return AdaptiveMeasurementWorkflow.builder()
                .classifierName(classifierName)
                .mode(AdaptiveMeasurementConfig.Mode.EXACT)
                .build().run();
    }

    public static AdaptiveMeasurementResult estimate(String classifierName) {
        return AdaptiveMeasurementWorkflow.builder()
                .classifierName(classifierName)
                .mode(AdaptiveMeasurementConfig.Mode.ADAPTIVE_STATIONARY)
                .build().run();
    }

    public static String scriptFor(AdaptiveMeasurementConfig config) {
        return ScriptGenerator.generate(config);
    }
}
