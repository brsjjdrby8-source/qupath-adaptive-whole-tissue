package org.sangha.qupath.adaptive.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.sangha.qupath.adaptive.controller.AdaptiveMeasurementWorkflow;
import org.sangha.qupath.adaptive.model.AdaptiveMeasurementConfig;
import org.sangha.qupath.adaptive.scripting.ScriptGenerator;
import qupath.lib.gui.QuPathGUI;

/** Minimal GUI surface with configuration -> Groovy round-trip. */
public final class AdaptiveMeasurementDialog {
    private final QuPathGUI qupath;

    public AdaptiveMeasurementDialog(QuPathGUI qupath) { this.qupath = qupath; }

    public void show() {
        var dialog = new Dialog<ButtonType>();
        dialog.setTitle("Adaptive whole-tissue measurement");
        dialog.initOwner(qupath.getStage());

        var classifier = new TextField("AT8 Classifier");
        var prefix = new TextField("WT");
        var chunk = new TextField("2000");
        var mode = new ComboBox<AdaptiveMeasurementConfig.Mode>();
        mode.getItems().setAll(AdaptiveMeasurementConfig.Mode.values());
        mode.setValue(AdaptiveMeasurementConfig.Mode.ADAPTIVE_STATIONARY);
        var batch = new Spinner<Integer>(1, 1024, 16);
        var ci = new TextField("0.0025");
        var drift = new TextField("0.0010");
        var stable = new Spinner<Integer>(1, 100, 3);
        var minChunks = new Spinner<Integer>(2, 1000000, 32);
        var binsX = new Spinner<Integer>(1, 64, 4);
        var binsY = new Spinner<Integer>(1, 64, 4);
        var seed = new TextField("42");

        var grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(12));
        int r = 0;
        add(grid, r++, "Classifier", classifier);
        add(grid, r++, "Annotation prefix", prefix);
        add(grid, r++, "Chunk size (µm)", chunk);
        add(grid, r++, "Mode", mode);
        add(grid, r++, "Batch size", batch);
        add(grid, r++, "CI half-width tolerance", ci);
        add(grid, r++, "Batch drift tolerance", drift);
        add(grid, r++, "Stable batches", stable);
        add(grid, r++, "Minimum chunks", minChunks);
        add(grid, r++, "Spatial bins X", binsX);
        add(grid, r++, "Spatial bins Y", binsY);
        add(grid, r++, "Seed", seed);

        dialog.getDialogPane().setContent(grid);
        var copyType = new ButtonType("Copy as Script", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(copyType, ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.CANCEL) return null;
            var cfg = AdaptiveMeasurementConfig.builder()
                    .classifierName(classifier.getText().trim())
                    .annotationPrefix(prefix.getText().trim())
                    .chunkSizeMicrons(Double.parseDouble(chunk.getText().trim()))
                    .mode(mode.getValue())
                    .batchSize(batch.getValue())
                    .absoluteTolerance(Double.parseDouble(ci.getText().trim()))
                    .driftTolerance(Double.parseDouble(drift.getText().trim()))
                    .stableBatches(stable.getValue())
                    .minimumChunks(minChunks.getValue())
                    .spatialBins(binsX.getValue(), binsY.getValue())
                    .seed(Long.parseLong(seed.getText().trim()))
                    .build();
            if (bt == copyType) {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(ScriptGenerator.generate(cfg));
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                return null;
            }
            AdaptiveMeasurementWorkflow.builder()
                    .classifierName(cfg.classifierName())
                    .annotationPrefix(cfg.annotationPrefix())
                    .chunkSizeMicrons(cfg.chunkSizeMicrons())
                    .mode(cfg.mode())
                    .batchSize(cfg.batchSize())
                    .confidenceLevel(cfg.confidenceLevel())
                    .absoluteTolerance(cfg.absoluteTolerance())
                    .driftTolerance(cfg.driftTolerance())
                    .stableBatches(cfg.stableBatches())
                    .minimumChunks(cfg.minimumChunks())
                    .spatialBins(cfg.spatialBinsX(), cfg.spatialBinsY())
                    .seed(cfg.seed())
                    .build().run();
            return null;
        });
        dialog.showAndWait();
    }

    private static void add(GridPane g, int row, String label, javafx.scene.Node control) {
        g.add(new Label(label), 0, row);
        g.add(control, 1, row);
    }
}
