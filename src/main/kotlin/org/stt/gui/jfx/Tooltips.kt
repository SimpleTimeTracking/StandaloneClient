package org.stt.gui.jfx

import javafx.scene.Node
import javafx.scene.control.Tooltip

internal object Tooltips {

    fun install(node: Node, tooltip: String) {
        Tooltip.install(node, Tooltip(tooltip))
    }
}
