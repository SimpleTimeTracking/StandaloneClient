package org.stt.gui.jfx;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.stt.model.TimeTrackingItem;

import javax.inject.Inject;
import java.util.Objects;
import java.util.ResourceBundle;

public class STTOptionDialogs {
    private ResourceBundle localization;

    @Inject
    public STTOptionDialogs(ResourceBundle localization) {

        this.localization = Objects.requireNonNull(localization);
    }

    Result showDeleteOrKeepDialog(TimeTrackingItem item) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setHeaderText(localization.getString("deleteItem.title"));
        dialog.setContentText(String.format(localization.getString("deleteItem.text"), item.getActivity()));
        ButtonType apply = new ButtonType(localization.getString("delete"), ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);
        dialog.setResultConverter(param -> param == apply ? Result.PERFORM_ACTION : Result.ABORT);
        return dialog.showAndWait()
                .orElse(Result.ABORT);
    }

    Result showNoCurrentItemAndItemIsLaterDialog() {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setHeaderText(localization.getString("strangeItem.title"));
        dialog.setContentText(localization.getString("noCurrentItemWithLateItem.text"));
        ButtonType apply = new ButtonType(localization.getString("add"), ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);
        dialog.setResultConverter(param -> param == apply ? Result.PERFORM_ACTION : Result.ABORT);
        return dialog.showAndWait()
                .orElse(Result.ABORT);
    }

    Result showItemCoversOtherItemsDialog(int numberOfCoveredItems) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setHeaderText(localization.getString("strangeItem.title"));
        dialog.setContentText(String.format(localization.getString("itemCoversOtherItems.text"), numberOfCoveredItems));
        ButtonType apply = new ButtonType(localization.getString("add"), ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);
        dialog.setResultConverter(param -> param == apply ? Result.PERFORM_ACTION : Result.ABORT);
        return dialog.showAndWait()
                .orElse(Result.ABORT);
    }

    public enum Result {
        PERFORM_ACTION, ABORT
    }
}
