package org.stt.gui.jfx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.stt.model.TimeTrackingItem;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class STTOptionDialogs {
    private ResourceBundle localization;

    @Inject
    public STTOptionDialogs(ResourceBundle localization) {

        this.localization = Objects.requireNonNull(localization);
    }

    Result showDeleteOrKeepDialog(Window parent, TimeTrackingItem item) {
        OptionDialogBuilder<Result> dialogBuilder = new OptionDialogBuilder<>();
        Button deleteButton = new Button(localization.getString("delete"));
        final Button keepButton = new Button(localization.getString("keep"));
        dialogBuilder.addButton(deleteButton, Result.PERFORM_ACTION);
        dialogBuilder.addDefaultButton(keepButton, Result.ABORT);

        return dialogBuilder.showAndGetResult(parent, localization.getString("deleteItem.title"),
                String.format(localization.getString("deleteItem.text"), item.getActivity()));
    }

    Result showNoCurrentItemAndItemIsLaterDialog(Window parent) {
        OptionDialogBuilder<Result> dialogBuilder = new OptionDialogBuilder<>();
        Button addButton = new Button(localization.getString("add"));
        final Button abortButton = new Button(localization.getString("abort"));
        dialogBuilder.addButton(addButton, Result.PERFORM_ACTION);
        dialogBuilder.addDefaultButton(abortButton, Result.ABORT);

        return dialogBuilder.showAndGetResult(parent, localization.getString("strangeItem.title"),
                localization.getString("noCurrentItemWithLateItem.text"));
    }

    Result showItemCoversOtherItemsDialog(Stage parent, int numberOfCoveredItems) {
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
        private T result;

        void addDefaultButton(Button button, final T value) {
            addButton(button, value);
            button.setCancelButton(true);
            button.setDefaultButton(true);
        }

        void addButton(final Button button, final T value) {
            button.setOnAction(event -> {
                stage.close();
                result = value;
            });
            button.setMnemonicParsing(true);
            buttons.add(button);
        }

        T showAndGetResult(Window parent, String title, String message) {
            Pane root;
            Label messageLabel;
            ButtonBar buttonContainer;
            try {
                root = FXMLLoader.load(getClass().getResource("/org/stt/gui/jfx/OptionDialog.fxml"));
                messageLabel = (Label) root.lookup("#message");
                buttonContainer = (ButtonBar) root.lookup("#buttonContainer");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            stage.setTitle(title);
            messageLabel.setText(message);

            buttonContainer.getButtons().addAll(buttons);

            Scene scene = new Scene(root);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(parent);
            stage.setScene(scene);
            stage.showAndWait();
            return getResult();
        }

        public T getResult() {
            return result;
        }
    }

    public enum Result {
        PERFORM_ACTION, ABORT
    }
}
