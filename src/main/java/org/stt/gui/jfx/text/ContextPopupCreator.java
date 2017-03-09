package org.stt.gui.jfx.text;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;

public class ContextPopupCreator {
    public static <T> Popup createPopupForContextMenu(final ListView<T> content, final ItemSelectionCallback<T> callback) {
        final Popup popup = new Popup();
//        popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
        popup.getContent().add(content);
//        content.setFixedCellSize(24);
        content.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            } else if (event.getCode() == KeyCode.ENTER) {
                callback.selected(content.getSelectionModel().getSelectedItem());
            }
        });
        content.setOnMouseClicked(event -> callback.selected(content.getSelectionModel().getSelectedItem()));
        content.prefHeightProperty().bind(Bindings.min(Bindings.size(content.itemsProperty().get()), 5).multiply(24).add(2));
        content.itemsProperty().get().addListener((ListChangeListener<T>) c -> {
            if (content.getSelectionModel().getSelectedItem() == null) {
                content.getSelectionModel().select(0);
            }
        });
        return popup;
    }

    @FunctionalInterface
    public interface ItemSelectionCallback<T> {
        void selected(T item);
    }
}
