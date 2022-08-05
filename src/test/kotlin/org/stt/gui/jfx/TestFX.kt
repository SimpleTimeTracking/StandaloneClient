package org.stt.gui.jfx


import com.sun.javafx.application.PlatformImpl
import com.sun.javafx.tk.Toolkit
import com.sun.scenario.DelayedRunnable
import com.sun.scenario.animation.AbstractMasterTimer
import javafx.scene.text.Font
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.stt.Matchers.any
import java.io.InputStream

object TestFX {

    /**
     * Monocle light
     */
    fun installTK() {
        try {
            val toolkitField = Toolkit::class.java.getDeclaredField("TOOLKIT")
            toolkitField.isAccessible = true
            val toolkit = mock(Toolkit::class.java, RETURNS_DEEP_STUBS)

            willAnswer { invocation ->
                val run = invocation.arguments[0] as Runnable
                run.run()
                null
            }.given(toolkit).startup(any())
            given(toolkit.masterTimer).willReturn(object : AbstractMasterTimer() {
                override fun postUpdateAnimationRunnable(animationRunnable: DelayedRunnable?) {}

                override fun getPulseDuration(precision: Int): Int {
                    return 1
                }
            })
            given(toolkit.fontLoader.loadFont(any<InputStream>(), anyDouble(), anyBoolean())).willAnswer { arrayOf( Font(1.0) ) }
            val layout = toolkit.textLayoutFactory.createLayout()
            //            given(layout.getBounds()).willReturn(new RectBounds(0, 0, 10, 10));
            given(layout.runs).willReturn(arrayOfNulls(0))
            given(layout.getCaretShape(anyInt(), anyBoolean(), anyFloat(), anyFloat())).willReturn(arrayOfNulls(0))
            given(layout.lines).willReturn(arrayOfNulls(0))
            given(layout.getRange(anyInt(), anyInt(), anyInt(), anyFloat(), anyFloat())).willReturn(arrayOfNulls(0))
            toolkitField.set(Toolkit::class.java, toolkit)
            toolkitField.isAccessible = false
            PlatformImpl.startup { }
        } catch (e: NoSuchFieldException) {
            throw AssertionError(e)
        } catch (e: IllegalAccessException) {
            throw AssertionError(e)
        }

    }
}
