package org.stt.gui.jfx.binding;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.stt.time.DateTimes;

import java.time.Duration;

public class STTBindings {
    private STTBindings() {
    }

    public static ObservableStringValue formattedDuration(final ObservableValue<Duration> duration) {
        return Bindings.createStringBinding(() -> DateTimes.FORMATTER_PERIOD_HHh_MMm_SSs.print(duration.getValue()), duration);
    }


    public static void bidirectionBindTextArea(StyleClassedTextArea commandText, StringProperty currentCommand) {
        currentCommand.addListener((observable, oldValue, newValue) -> {
            if (!commandText.getText().equals(newValue)) {
                commandText.replaceText(newValue);
            }
        });
        commandText.textProperty().
                addListener((observable, oldValue, newValue) -> {
                    if (!currentCommand.get().equals(newValue)) {
                        currentCommand.setValue(newValue);
                    }
                });
    }

    public static void bidirectionBindCaretPosition(StyleClassedTextArea commandText, IntegerProperty commandCaretPosition) {
        commandCaretPosition.addListener(observable -> {
            if (commandText.getCaretPosition() != commandCaretPosition.get()) {
                commandText.moveTo(commandCaretPosition.get());
            }
        });
        commandText.caretPositionProperty().
                addListener(
                        observable -> {
                            if (commandText.getCaretPosition() != commandCaretPosition.get()) {
                                commandCaretPosition.set(commandText
                                        .getCaretPosition());
                            }
                        });
    }
}
