package org.stt.gui.jfx

import javafx.scene.Node
import javafx.scene.control.Button

internal class FramelessButton(node: Node) : Button() {

    private var releasedStyle = STYLE_NORMAL

    init {
        graphic = node
        style = STYLE_NORMAL

        setOnMousePressed { style = STYLE_PRESSED }

        setOnMouseReleased { style = releasedStyle }

        setOnMouseEntered { setStyleAndReleaseStyle(STYLE_HOVER) }
        setOnMouseExited { setStyleAndReleaseStyle(STYLE_NORMAL) }
    }

    private fun setStyleAndReleaseStyle(style: String) {
        setStyle(style)
        releasedStyle = style
    }

    companion object {
        private const val STYLE_HOVER = "-fx-background-color: transparent; -fx-padding: 5;  -fx-effect: innershadow(gaussian, rgba(60, 100, 220, 0.8), 20, 0.5, 2, 2);"
        private const val STYLE_NORMAL = "-fx-background-color: transparent; -fx-padding: 5; -fx-effect: null"
        private const val STYLE_PRESSED = "-fx-background-color: transparent; -fx-padding: 7 4 3 6; -fx-effect: innershadow(gaussian, rgba(60, 100, 220, 0.8), 20, 0.5, 2, 2);"
    }
}
