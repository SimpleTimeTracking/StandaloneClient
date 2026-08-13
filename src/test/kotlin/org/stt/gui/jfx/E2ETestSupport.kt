package org.stt.gui.jfx

import org.stt.model.TimeTrackingItem
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*

/**
 * Shared infrastructure for TestFX end-to-end tests.
 *
 * Provides headless Monocle configuration (so tests run without a physical display)
 * and a temp-file helper so each test gets an isolated persistence layer.
 *
 * @see ReportE2ETest
 */
object E2ETestSupport {

    /**
     * Configures the JVM for headless GUI testing:
     * - Sets [Locale.GERMANY] so `DateTimeFormatter` uses 24-hour format,
     *   allowing ANTLR to parse times like `"09:00"` without AM/PM.
     * - Enables Monocle headless mode so JavaFX runs without a display.
     */
    fun setup() {
        Locale.setDefault(Locale.GERMANY)

        System.setProperty("java.awt.headless", "true")
        System.setProperty("testfx.headless", "true")
        System.setProperty("glass.platform", "Monocle")
        System.setProperty("monocle.platform", "Headless")
        System.setProperty("prism.order", "sw")
        System.setProperty("prism.text", "t2k")
    }

    /**
     * Creates a temporary directory and returns a [Path] to an `activities` file
     * inside it. The file is not created here — the [STTItemPersister] will create
     * it on first write.
     *
     * @return a [Path] to `{tempDir}/activities`
     */
    fun createTempActivitiesFile(): Path {
        val dir = Files.createTempDirectory("stt-e2e")
        return dir.resolve("activities")
    }

    /**
     * Factory helper for constructing a [TimeTrackingItem] with the given activity
     * name, start time, and optional end time.
     *
     * @param activity the activity/comment text
     * @param start    start date-time
     * @param end      optional end date-time (null = still running)
     * @return a new [TimeTrackingItem]
     */
    fun item(activity: String, start: LocalDateTime, end: LocalDateTime? = null): TimeTrackingItem {
        return TimeTrackingItem(activity, start, end)
    }
}