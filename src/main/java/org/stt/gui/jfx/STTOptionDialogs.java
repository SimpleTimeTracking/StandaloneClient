package org.stt.gui.jfx;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import org.stt.model.TimeTrackingItem;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

public class STTOptionDialogs {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private ResourceBundle localization;
    private final Font glyphFont;
    private final ActivityTextDisplayProcessor labelToNodeMapper;

    @Inject
    public STTOptionDialogs(ResourceBundle localization,
                            @Named("glyph") javafx.scene.text.Font glyphFont,
                            @Named("activityToText") ActivityTextDisplayProcessor labelToNodeMapper) {
        this.localization = requireNonNull(localization);
        this.glyphFont = requireNonNull(glyphFont);
        this.labelToNodeMapper = requireNonNull(labelToNodeMapper);
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

    Result showItemCoversOtherItemsDialog(List<TimeTrackingItem> coveredItems) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setHeaderText(localization.getString("strangeItem.title"));
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setPrefWidth(800);
        dialogPane.getStylesheets().add("org/stt/gui/jfx/STT.css");

        ListView<TimeTrackingItem> coveredItemsList = new ListView<>();
        coveredItemsList.setCellFactory(listview -> {
            TimeTrackingItemNodes itemNodes = new TimeTrackingItemNodes(labelToNodeMapper, DATE_TIME_FORMATTER, glyphFont, 400, 360, localization);
            ListCell<TimeTrackingItem> cell = new ListCell<TimeTrackingItem>() {
                @Override
                protected void updateItem(TimeTrackingItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        itemNodes.setItem(item);
                    }
                }
            };
            HBox cellParts = new HBox();
            cellParts.setAlignment(Pos.CENTER_LEFT);
            cell.setGraphic(cellParts);
            itemNodes.appendNodesTo(cellParts.getChildren());
            return cell;
        });
        coveredItemsList.getItems().setAll(coveredItems);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new Label(localization.getString("itemCoversOtherItems.text")));
        borderPane.setCenter(coveredItemsList);

        dialogPane.setContent(borderPane);
        ButtonType apply = new ButtonType(localization.getString("continue"), ButtonBar.ButtonData.APPLY);
        dialogPane.getButtonTypes().addAll(apply, ButtonType.CANCEL);
        dialog.setResultConverter(param -> param == apply ? Result.PERFORM_ACTION : Result.ABORT);
        return dialog.showAndWait()
                .orElse(Result.ABORT);
    }

    Result showRenameDialog(int numberOfActivities, String before, String after) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setHeaderText(localization.getString("bulkRename.title"));
        dialog.getDialogPane().setContent(new Label(String.format(localization.getString("bulkRename.text"), numberOfActivities, before, after)));
        ButtonType apply = new ButtonType(localization.getString("rename"), ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.NO);
        dialog.setResultConverter(param -> param == apply ? Result.PERFORM_ACTION : Result.ABORT);
        return dialog.showAndWait()
                .orElse(Result.ABORT);
    }

    public enum Result {
        PERFORM_ACTION, ABORT
    }
}
