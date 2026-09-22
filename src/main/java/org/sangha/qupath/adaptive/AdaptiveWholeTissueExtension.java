package org.sangha.qupath.adaptive;

import javafx.scene.control.MenuItem;
import org.sangha.qupath.adaptive.ui.AdaptiveMeasurementDialog;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

public final class AdaptiveWholeTissueExtension implements QuPathExtension {
    @Override
    public void installExtension(QuPathGUI qupath) {
        var menu = qupath.getMenu("Extensions>Adaptive whole tissue", true);
        var item = new MenuItem("Measure / estimate...");
        item.setOnAction(e -> new AdaptiveMeasurementDialog(qupath).show());
        menu.getItems().add(item);
    }

    @Override
    public String getName() { return "Adaptive whole-tissue measurement"; }

    @Override
    public String getDescription() {
        return "Exact and statistically controlled adaptive measurement of whole-tissue pixel-classifier burden.";
    }
}
