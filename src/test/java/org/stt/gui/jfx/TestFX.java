package org.stt.gui.jfx;


import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractMasterTimer;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.lang.reflect.Field;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
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
            toolkitField.set(Toolkit.class, toolkit);
            toolkitField.setAccessible(false);
            PlatformImpl.startup(() -> {});
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
