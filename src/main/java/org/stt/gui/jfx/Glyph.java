package org.stt.gui.jfx;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

public enum Glyph {
    ANGLE_DOUBLE_DOWN('\uf103'),
    ANGLE_DOUBLE_UP('\uf102'),
    CALENDAR('\uf073'),
    CHEVRON_CIRCLE_RIGHT('\uf138'),
    CLIPBOARD('\uf0ea'),
    PENCIL('\uf040'),
    STOP_CIRCLE('\uf28d'),
    PLAY_CIRCLE('\uf144'),
    PLUS_CIRCLE('\uf055'),
    TRASH('\uf1f8'),
    FAST_FORWARD('\uf050'),
    FORWARD('\uf04e');

    private static final Color GLYPH_COLOR = Color.GRAY;
    private final char code;


    Glyph(char code) {
        this.code = code;
    }

    public String getCode() {
        return Character.toString(code);
    }

    public static Label glyph(Font fontAwesome, Glyph glyph, double size) {
        Label label = new Label(glyph.getCode());
        Font font = Font.font(fontAwesome.getFamily(), size);
        label.setFont(font);
        label.setTextFill(GLYPH_COLOR);
        return label;
    }

    public static Label glyph(Font fontAwesome, Glyph glyph, double size, Paint paint) {
        Label label = new Label(glyph.getCode());
        Font font = Font.font(fontAwesome.getFamily(), size);
        label.setFont(font);
        label.setTextFill(paint);
        return label;
    }

    public static Label glyph(Font fontAwesome, Glyph glyph) {
        Label label = new Label(glyph.getCode());
        label.setFont(fontAwesome);
        label.setTextFill(GLYPH_COLOR);
        return label;
    }
}
