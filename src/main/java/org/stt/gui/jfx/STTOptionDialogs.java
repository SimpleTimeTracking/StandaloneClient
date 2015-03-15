package org.stt.gui.jfx;

import com.google.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.stt.model.TimeTrackingItem;

import java.io.IOException;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 12.03.15.
 */
public class STTOptionDialogs {
    private ResourceBundle localization;

    @Inject
    public STTOptionDialogs(ResourceBundle localization) {

        this.localization = checkNotNull(localization);
    }

    public Result showDeleteOrKeepDialog(Window parent, TimeTrackingItem item) {
        Stage deleteCancelStage = new Stage();

        final SimpleObjectProperty<Result> resultProperty = new SimpleObjectProperty<>(null);

        Button deleteButton = new Button(localization.getString("delete"));
        setupButton(deleteButton, resultProperty, Result.DELETE);
        final Button keepButton = new Button(localization.getString("keep"));
        setupDefaultButton(keepButton, resultProperty, Result.KEEP);

        showOptionDialog(parent, deleteCancelStage, localization.getString("delete.item.title"),
                String.format(localization.getString("delete.item.text"), item.getComment().or("")), deleteButton, keepButton);
        return resultProperty.getValue();
    }

    private void showOptionDialog(Window parent, final Stage stage, String title, String message, Button... buttons) {
        VBox vbox;
        Label messageLabel;
        HBox buttonContainer;
        try {
            vbox = FXMLLoader.load(getClass().getResource("/org/stt/gui/jfx/OptionDialog.fxml"));
            messageLabel = (Label) vbox.lookup("#message");
            buttonContainer = (HBox) vbox.lookup("#buttonContainer");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        stage.setTitle(title);
        messageLabel.setText(message);

        buttonContainer.getChildren().addAll(buttons);

        Scene scene = new Scene(vbox);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parent);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private <T> void setupDefaultButton(Button button, final SimpleObjectProperty<T> resultProperty, final T value) {
        setupButton(button, resultProperty, value);
        button.setCancelButton(true);
        button.setDefaultButton(true);
    }

    private <T> void setupButton(final Button button, final SimpleObjectProperty<T> resultProperty, final T value) {
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ((Stage) button.getScene().getWindow()).close();
                resultProperty.setValue(value);
            }
        });
        button.setMnemonicParsing(true);
    }

    public enum Result {
        DELETE, KEEP;
    }
}
