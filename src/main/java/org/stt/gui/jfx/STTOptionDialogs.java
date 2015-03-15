package org.stt.gui.jfx;

import com.google.inject.Inject;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
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
import java.util.ArrayList;
import java.util.List;
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
        OptionDialogBuilder<Result> dialogBuilder = new OptionDialogBuilder<>();
        Button deleteButton = new Button(localization.getString("delete"));
        final Button keepButton = new Button(localization.getString("keep"));
        dialogBuilder.addButton(deleteButton, Result.PERFORM_ACTION);
        dialogBuilder.addDefaultButton(keepButton, Result.ABORT);

        return dialogBuilder.showAndGetResult(parent, localization.getString("deleteItem.title"),
                String.format(localization.getString("deleteItem.text"), item.getComment().or("")));
    }

    public Result showNoCurrentItemAndItemIsLaterDialog(Window parent) {
        OptionDialogBuilder<Result> dialogBuilder = new OptionDialogBuilder<>();
        Button addButton = new Button(localization.getString("add"));
        final Button abortButton = new Button(localization.getString("abort"));
        dialogBuilder.addButton(addButton, Result.PERFORM_ACTION);
        dialogBuilder.addDefaultButton(abortButton, Result.ABORT);

        return dialogBuilder.showAndGetResult(parent, localization.getString("strangeItem.title"),
                localization.getString("noCurrentItemWithLateItem.text"));
    }

    public Result showItemCoversOtherItemsDialog(Stage parent, int numberOfCoveredItems) {
        OptionDialogBuilder<Result> dialogBuilder = new OptionDialogBuilder<>();
        Button addButton = new Button(localization.getString("add"));
        final Button abortButton = new Button(localization.getString("abort"));
        dialogBuilder.addButton(addButton, Result.PERFORM_ACTION);
        dialogBuilder.addDefaultButton(abortButton, Result.ABORT);

        return dialogBuilder.showAndGetResult(parent, localization.getString("strangeItem.title"),
                String.format(localization.getString("itemCoversOtherItems.text"), numberOfCoveredItems));
    }


    private static class OptionDialogBuilder<T> {
        private Stage stage = new Stage();
        private List<Button> buttons = new ArrayList<>();
        public T result;

        public void addDefaultButton(Button button, final T value) {
            addButton(button, value);
            button.setCancelButton(true);
            button.setDefaultButton(true);
        }

        public void addButton(final Button button, final T value) {
            button.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                stage.close();
                result = value;
                }
            });
            button.setMnemonicParsing(true);
            buttons.add(button);
        }

        public T showAndGetResult(Window parent, String title, String message) {
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
            return result;
        }
    }

    public enum Result {
        PERFORM_ACTION, ABORT;
    }
}
