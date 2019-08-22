package org.stt.gui.jfx.text

import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.control.TextArea
import javafx.stage.Popup
import javafx.stage.Window

class PopupAtCaretPlacer(textArea: TextArea, private val popup: Popup) {
    private val path: Node = textArea.lookup("Path")
    private val window: Window

    init {
        this.window = textArea.scene.window

        path.boundsInLocalProperty().addListener { _, _, newValue -> updatePopupLocation(newValue) }
        updatePopupLocation(path.boundsInLocal)
    }

    private fun updatePopupLocation(bounds: Bounds) {
        if (popup.isShowing) {
            val caretPosition = path.localToScene(bounds)
            val x = window.x + window.scene.x
            val y = window.y + window.scene.y
            popup.x = caretPosition.maxX + x
            popup.y = caretPosition.maxY + y
        }
    }
}
