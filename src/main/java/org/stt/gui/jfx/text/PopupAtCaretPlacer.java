package org.stt.gui.jfx.text;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.Objects;

public class PopupAtCaretPlacer {
    private final Node path;
    private final Popup popup;
    private final Window window;

    public PopupAtCaretPlacer(final TextArea textArea, final Popup popup) {
        Objects.requireNonNull(textArea);
        this.popup = Objects.requireNonNull(popup);
        path = textArea.lookup("Path");
        this.window = textArea.getScene().getWindow();

        path.boundsInLocalProperty().addListener((observable, oldValue, newValue) -> updatePopupLocation(newValue));
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
