package org.stt.gui.jfx.text;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;

/**
 * Created by dante on 13.12.14.
 */
public class ContextPopupCreator {
    public static <T> Popup createPopupForContextMenu(final ListView<T> content, final ItemSelectionCallback<T> callback) {
        final Popup popup = new Popup();
//        popup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
        popup.getContent().add(content);
//        content.setFixedCellSize(24);
        content.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    popup.hide();
                } else if (event.getCode() == KeyCode.ENTER) {
                    callback.selected(content.getSelectionModel().getSelectedItem());
                }
            }
        });
        content.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                callback.selected(content.getSelectionModel().getSelectedItem());
            }
        });
        content.prefHeightProperty().bind(Bindings.min(Bindings.size(content.itemsProperty().get()), 5).multiply(24).add(2));
        content.itemsProperty().get().addListener(new ListChangeListener<T>() {
            @Override
            public void onChanged(Change<? extends T> c) {
                if (content.getSelectionModel().getSelectedItem() == null) {
                    content.getSelectionModel().select(0);
                }
            }
        });
        return popup;
    }

    public interface ItemSelectionCallback<T> {
        void selected(T item);
    }
}
