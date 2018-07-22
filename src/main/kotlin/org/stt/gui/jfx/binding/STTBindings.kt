package org.stt.gui.jfx.binding

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableStringValue
import javafx.beans.value.ObservableValue
import org.stt.time.DateTimes

import java.time.Duration
import java.util.concurrent.Callable

object STTBindings {

    fun formattedDuration(duration: ObservableValue<Duration>): ObservableStringValue {
        return Bindings.createStringBinding(Callable { DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs(duration.value) }, duration)
    }
}
