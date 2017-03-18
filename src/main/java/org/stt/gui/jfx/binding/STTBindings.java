package org.stt.gui.jfx.binding;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import org.stt.time.DateTimes;

import java.time.Duration;

public class STTBindings {
    private STTBindings() {
    }

    public static ObservableStringValue formattedDuration(final ObservableValue<Duration> duration) {
        return Bindings.createStringBinding(() -> DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs.print(duration.getValue()), duration);
    }
}
