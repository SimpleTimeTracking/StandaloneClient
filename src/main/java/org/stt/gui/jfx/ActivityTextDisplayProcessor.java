package org.stt.gui.jfx;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Allows "pimping" the display of an activity. Should return a Stream of {@link javafx.scene.Node}s or Strings if another
 * processor should handle it.
 */
public interface ActivityTextDisplayProcessor extends Function<Object, Stream<Object>> {
}
