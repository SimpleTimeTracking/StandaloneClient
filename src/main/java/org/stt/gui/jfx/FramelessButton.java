package org.stt.gui.jfx;

import javafx.scene.Node;
import javafx.scene.control.Button;

import java.util.Objects;

class FramelessButton extends Button {
    private static final String STYLE_HOVER = "-fx-background-color: transparent; -fx-padding: 5;  -fx-effect: innershadow(gaussian, rgba(60, 100, 220, 0.8), 20, 0.5, 2, 2);";
    private static final String STYLE_NORMAL = "-fx-background-color: transparent; -fx-padding: 5; -fx-effect: null";
    private static final String STYLE_PRESSED = "-fx-background-color: transparent; -fx-padding: 7 4 3 6; -fx-effect: innershadow(gaussian, rgba(60, 100, 220, 0.8), 20, 0.5, 2, 2);";

    private String releasedStyle = STYLE_NORMAL;

    FramelessButton(Node node) {
        Objects.requireNonNull(node);
        setGraphic(node);
        setStyle(STYLE_NORMAL);

        setOnMousePressed(event -> setStyle(STYLE_PRESSED));

        setOnMouseReleased(event -> setStyle(releasedStyle));

        setOnMouseEntered(event -> setStyleAndReleaseStyle(STYLE_HOVER));
        setOnMouseExited(event -> setStyleAndReleaseStyle(STYLE_NORMAL));
    }

    private void setStyleAndReleaseStyle(String style) {
        setStyle(style);
        releasedStyle = style;
    }
}
