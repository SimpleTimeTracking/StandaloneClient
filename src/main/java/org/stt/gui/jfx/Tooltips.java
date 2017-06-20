package org.stt.gui.jfx;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

class Tooltips {
    private Tooltips() {
    }

    static void install(Node node, String tooltip) {
        Tooltip.install(node, new Tooltip(tooltip));
    }
}
