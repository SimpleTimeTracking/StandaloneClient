package org.stt.gui.jfx

import javafx.scene.control.Label
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font

enum class Glyph(private val code: Char) {
    ANGLE_DOUBLE_DOWN('\uf103'),
    ANGLE_DOUBLE_LEFT('\uf100'),
    ANGLE_DOUBLE_RIGHT('\uf101'),
    ANGLE_DOUBLE_UP('\uf102'),
    ANGLE_LEFT('\uf104'),
    ANGLE_RIGHT('\uf105'),
    ARROW_CIRCLE_RIGHT('\uf0a9'),
    CALENDAR('\uf073'),
    CHECK('\uf00c'),
    CHEVRON_CIRCLE_RIGHT('\uf138'),
    CLIPBOARD('\uf0ea'),
    PENCIL('\uf040'),
    STOP_CIRCLE('\uf28d'),
    PLAY_CIRCLE('\uf144'),
    PLUS_CIRCLE('\uf055'),
    TRASH('\uf1f8'),
    FAST_FORWARD('\uf050'),
    FORWARD('\uf04e');

    fun getCode(): String {
        return Character.toString(code)
    }

    companion object {

        const val GLYPH_SIZE_MEDIUM = 20.0
        const val GLYPH_SIZE_LARGE = 30.0
        private val GLYPH_COLOR = Color.GRAY

        fun glyph(fontAwesome: Font, glyph: Glyph, size: Double): Label {
            val label = Label(glyph.getCode())
            val font = Font.font(fontAwesome.family, size)
            label.font = font
            label.textFill = GLYPH_COLOR
            return label
        }

        fun glyph(fontAwesome: Font, glyph: Glyph, size: Double, paint: Paint): Label {
            val label = Label(glyph.getCode())
            val font = Font.font(fontAwesome.family, size)
            label.font = font
            label.textFill = paint
            return label
        }

        fun glyph(fontAwesome: Font, glyph: Glyph): Label {
            val label = Label(glyph.getCode())
            label.font = fontAwesome
            label.textFill = GLYPH_COLOR
            return label
        }
    }
}
