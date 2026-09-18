package org.stt.gui.jfx

import javafx.stage.Stage
import org.controlsfx.control.Notifications
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Shows ControlsFX notifications to the user, anchored to the owner window
 * returned by [ownerProvider] when one is available and showing. Anchoring
 * the notification to the main window makes the popup appear reliably on all
 * supported platforms. Failures to display are logged instead of propagated,
 * so notification display can never break the calling action.
 */
internal class UserNotifier(private val ownerProvider: () -> Stage?) {

    /**
     * Shows the given message. Uses the owner window when available, falls
     * back to an ownerless notification otherwise.
     *
     * The message is wrapped at word boundaries: the ControlsFX notification
     * bar grows with unwrapped text and is positioned using its width
     * assumed at show time, so long messages would extend beyond the owner
     * window to the right. Keeping lines short bounds the bar width.
     */
    fun show(message: String) {
        try {
            val notification = Notifications.create().text(wrapText(message))
            ownerProvider()?.let { notification.owner(it) }
            notification.show()
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Could not show user notification", e)
        }
    }

    internal fun wrapText(message: String, maxLineLength: Int = 60): String {
        val result = StringBuilder()
        for (paragraph in message.split("\n")) {
            var lineLength = 0
            for (word in paragraph.split(" ")) {
                if (lineLength > 0 && lineLength + 1 + word.length > maxLineLength) {
                    result.append("\n")
                    lineLength = 0
                } else if (lineLength > 0) {
                    result.append(" ")
                    lineLength++
                }
                result.append(word)
                lineLength += word.length
            }
            result.append("\n")
        }
        return result.toString().trimEnd('\n')
    }

    companion object {
        private val LOG = Logger.getLogger(UserNotifier::class.java.name)
    }
}
