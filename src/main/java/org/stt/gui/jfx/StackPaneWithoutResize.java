package org.stt.gui.jfx;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Only GridPane allows to keep the preferred size of its children, and that's pretty heavy weight.
 * StackPane allows alignment of children, but tries to resize to fill. This class will not resize its children.
 */
public class StackPaneWithoutResize extends StackPane {
    public StackPaneWithoutResize(Node... children) {
        super(children);
    }

    @Override
    protected void layoutInArea(Node child, double areaX, double areaY,
                                double areaWidth, double areaHeight,
                                double areaBaselineOffset,
                                Insets margin,
                                HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight,
                areaBaselineOffset, margin, false, false, halignment, valignment);
    }

}
