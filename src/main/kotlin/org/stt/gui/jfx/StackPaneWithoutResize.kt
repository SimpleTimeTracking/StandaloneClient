package org.stt.gui.jfx

import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * Only GridPane allows to keep the preferred size of its children, and that's pretty heavy weight.
 * StackPane allows alignment of children, but tries to resize to fill. This class will not resize its children.
 */
class StackPaneWithoutResize(vararg children: Node) : StackPane(*children) {

    override fun layoutInArea(child: Node?, areaX: Double, areaY: Double,
                              areaWidth: Double, areaHeight: Double,
                              areaBaselineOffset: Double,
                              margin: Insets?,
                              halignment: HPos?, valignment: VPos?) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight,
                areaBaselineOffset, margin, false, false, halignment, valignment)
    }

}
