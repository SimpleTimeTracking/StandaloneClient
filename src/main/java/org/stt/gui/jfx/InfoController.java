package org.stt.gui.jfx;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ResourceBundle;

public class InfoController {
    private final Pane panel;

    @Inject
    public InfoController(ResourceBundle localization) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/ReportPanel.fxml"), localization);
        loader.setController(this);
        try {
            panel = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
