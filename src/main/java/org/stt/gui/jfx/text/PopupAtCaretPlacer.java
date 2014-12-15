package org.stt.gui.jfx.text;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.stage.Popup;
import javafx.stage.Window;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 13.12.14.
 */
public class PopupAtCaretPlacer {
    private final Node path;
    private final Popup popup;
    private final Window window;

    public PopupAtCaretPlacer(final TextArea textArea, final Popup popup) {
        checkNotNull(textArea);
        this.popup = checkNotNull(popup);
        path = textArea.lookup("Path");
        this.window = textArea.getScene().getWindow();

        path.boundsInLocalProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                updatePopupLocation(newValue);
            }
        });
        updatePopupLocation(path.getBoundsInLocal());
    }

    private void updatePopupLocation(Bounds bounds) {
        if (popup.isShowing()) {
            Bounds caretPosition = path.localToScene(bounds);
            double x = window.getX() + window.getScene().getX();
            double y = window.getY() + window.getScene().getY();
            popup.setX(caretPosition.getMaxX() + x);
            popup.setY(caretPosition.getMaxY() + y);
        }
    }
}
