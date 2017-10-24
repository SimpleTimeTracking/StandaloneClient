package org.stt.gui.jfx;


import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractMasterTimer;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.lang.reflect.Field;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class TestFX {
    private TestFX() {
    }

    /**
     * Monocle light
     */
    public static void installTK() {
        try {
            Field toolkitField = Toolkit.class.getDeclaredField("TOOLKIT");
            toolkitField.setAccessible(true);
            Toolkit toolkit = mock(Toolkit.class, RETURNS_DEEP_STUBS);

            willAnswer(invocation -> {
                Runnable run = (Runnable) invocation.getArguments()[0];
                run.run();
                return null;
            }).given(toolkit).startup(any(Runnable.class));
            given(toolkit.getMasterTimer()).willReturn(new AbstractMasterTimer() {
                @Override
                protected void postUpdateAnimationRunnable(DelayedRunnable animationRunnable) {
                }

                @Override
                protected int getPulseDuration(int precision) {
                    return 1;
                }
            });
            given(toolkit.getFontLoader().loadFont(any(InputStream.class), anyDouble())).willAnswer(invocation -> new Font(1));
            TextLayout layout = toolkit.getTextLayoutFactory().createLayout();
//            given(layout.getBounds()).willReturn(new RectBounds(0, 0, 10, 10));
            given(layout.getRuns()).willReturn(new GlyphList[0]);
            given(layout.getCaretShape(anyInt(), anyBoolean(), anyFloat(), anyFloat())).willReturn(new PathElement[0]);
            given(layout.getLines()).willReturn(new TextLine[0]);
            given(layout.getRange(anyInt(), anyInt(), anyInt(), anyFloat(), anyFloat())).willReturn(new PathElement[0]);
            toolkitField.set(Toolkit.class, toolkit);
            toolkitField.setAccessible(false);
            PlatformImpl.startup(() -> {});
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
