package org.stt.gui.jfx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.stt.update.UpdateChecker;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class InfoController {
    private static final Logger LOG = Logger.getLogger(InfoController.class.getName());
    public static final String URL_PROJECT = "https://github.com/SimpleTimeTracking/StandaloneClient/";
    private BorderPane panel;
    private final ResourceBundle localization;
    private final UpdateChecker updateChecker;

    @FXML
    private Button updateButton;
    @FXML
    private Label versionLabel;
    @FXML
    private Label hashValue;
    @FXML
    private VBox updateArea;
    private Font fontAwesome;
    private final String appVersion;
    private String commitHash;
    private final ExecutorService executorService;

    @Inject
    public InfoController(ResourceBundle localization,
                          UpdateChecker updateChecker,
                          @Named("glyph") Font fontAwesome,
                          @Named("version") String appVersion,
                          @Named("commit hash") String commitHash,
                          ExecutorService executorService) {
        this.localization = requireNonNull(localization);
        this.updateChecker = requireNonNull(updateChecker);
        this.fontAwesome = requireNonNull(fontAwesome);
        this.appVersion = requireNonNull(appVersion);
        this.commitHash = requireNonNull(commitHash);
        this.executorService = requireNonNull(executorService);
    }

    @FXML
    public void initialize() {
        versionLabel.setText(appVersion);
        hashValue.setText(commitHash);
    }

    public Pane getPanel() {
        loadAndInjectFXML();
        return panel;
    }

    private void loadAndInjectFXML() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/InfoPanel.fxml"), localization);
        loader.setController(this);
        try {
            panel = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FXML
    private void checkForUpdate() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(40, 40);
        updateArea.getChildren().set(0, progressIndicator);
        updateChecker.queryNewerVersion()
                .handleAsync((a, t) -> {
                    if (t != null) {
                        updateArea.getChildren().set(0, new Label(t.getMessage()));
                    }
                    if (a.isPresent()) {
                        updateArea.getChildren().set(0, new Label(String.format(localization.getString("info.newerVersion"), a.get()), Glyph.glyph(fontAwesome, Glyph.ANGLE_DOUBLE_UP)));
                    } else {
                        updateArea.getChildren().set(0, new Label(localization.getString("info.upToDate"), Glyph.glyph(fontAwesome, Glyph.CHECK)));
                    }
                    return null;
                }, Platform::runLater);
    }

    @FXML
    private void openHomepage() {
        executorService.execute(() -> {
            try {
                Desktop.getDesktop().browse(new URI(URL_PROJECT));
            } catch (IOException | URISyntaxException ex) {
                LOG.log(Level.SEVERE, "Couldn't open homepage", ex);
            }
        });
    }
}
