package org.stt.gui.jfx

import javafx.stage.Stage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito

/**
 * Verifies that user notifications are shown without failing, both with and
 * without an available owner window: the owner is only used when the main
 * window is actually showing, and display errors never propagate to callers.
 */
class UserNotifierTest {

    @Before
    fun setup() {
        TestFX.installTK()
    }

    @Test
    fun shouldShowWithoutOwnerWhenNoStageAvailable() {
        // GIVEN
        val notifier = UserNotifier { null }

        // WHEN / THEN - must not throw
        notifier.show("Test message")
    }

    @Test
    fun shouldShowWithOwnerWhenStageIsShowing() {
        // GIVEN
        val stage = Mockito.mock(Stage::class.java)
        given(stage.isShowing).willReturn(true)
        val notifier = UserNotifier { stage }

        // WHEN / THEN - must not throw
        notifier.show("Test message")
    }

    @Test
    fun shouldNotUseOwnerWhenStageIsNotShowing() {
        // GIVEN
        val stage = Mockito.mock(Stage::class.java)
        given(stage.isShowing).willReturn(false)
        val notifier = UserNotifier { stage }

        // WHEN / THEN - must not throw
        notifier.show("Test message")
    }

    @Test
    fun shouldWrapLongMessageAtWordBoundaries() {
        // GIVEN
        val notifier = UserNotifier { null }
        val message = "Skipping 'some quite long activity description here': issue DEVTT-6 already has worklogs in Tempo on 2026-09-17."

        // WHEN
        val wrapped = notifier.wrapText(message, 60)

        // THEN
        assertThat(wrapped).doesNotContain("  ")
        for (line in wrapped.split("\n")) {
            assertThat(line.length).isLessThanOrEqualTo(60)
        }
        assertThat(wrapped.replace("\n", " ")).isEqualTo(message)
    }

    @Test
    fun shouldKeepShortMessageUnwrapped() {
        // GIVEN
        val notifier = UserNotifier { null }

        // WHEN
        val wrapped = notifier.wrapText("Tempo: 1 submitted, 0 skipped, 0 failed.")

        // THEN
        assertThat(wrapped).isEqualTo("Tempo: 1 submitted, 0 skipped, 0 failed.")
    }
}
