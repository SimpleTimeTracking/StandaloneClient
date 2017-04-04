package org.stt.gui.jfx;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

public abstract class Tooltips {
    private Tooltips() {
    }

    public static void install(Node node, String tooltip) {
        Tooltip.install(node, new Tooltip(tooltip));
    }
}
