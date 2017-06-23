package org.stt.gui.jfx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.controlsfx.control.Notifications;
import org.stt.event.NotifyUser;
import org.stt.event.ShuttingDown;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

public class MainWindowController {

    private final Parent rootNode;
    private final ResourceBundle localization;
    private final ActivitiesController activitiesController;
    private final ReportController reportController;
    private final SettingsController settingsController;
    private final InfoController infoController;
    private final MBassador<Object> eventBus;

    @FXML
    private Tab activitiesTab;
    @FXML
    private Tab reportTab;
    @FXML
    private Tab settingsTab;
    @FXML
    private Tab infoTab;

    @Inject
    MainWindowController(ResourceBundle localization,
                         ActivitiesController activitiesController,
                         ReportController reportController,
                         MBassador<Object> eventBus,
                         SettingsController settingsController,
                         InfoController infoController) {
        this.localization = requireNonNull(localization);
        this.activitiesController = requireNonNull(activitiesController);
        this.reportController = requireNonNull(reportController);
        this.eventBus = requireNonNull(eventBus);
        this.settingsController = requireNonNull(settingsController);
        this.infoController = requireNonNull(infoController);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/MainWindow.fxml"), localization);
        loader.setController(this);

        try {
            rootNode = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        rootNode.getStylesheets().add("org/stt/gui/jfx/STT.css");

        eventBus.subscribe(this);
    }

    @Handler
    public void onUserNotifactionRequest(NotifyUser event) {
        Notifications.create().text(event.message).show();
    }

    @FXML
    public void initialize() {
        activitiesTab.setContent(activitiesController.getNode());
        reportTab.setContent(reportController.getPanel());
        settingsTab.setContent(settingsController.getPanel());
        infoTab.setContent(infoController.getPanel());
    }

    public void show(Stage stage) {
        Scene scene = new Scene(rootNode);

        stage.setOnCloseRequest(event -> Platform.runLater(this::shutdown));
        scene.setOnKeyPressed(event -> {
            if (KeyCode.ESCAPE.equals(event.getCode())) {
                event.consume();
                shutdown();
            }
        });

        Image applicationIcon = new Image("/Logo.png", 32, 32, true, true);

        stage.getIcons().add(applicationIcon);
        stage.setTitle(localization.getString("window.title"));
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }

    private void shutdown() {
        eventBus.publish(new ShuttingDown());
    }
}
