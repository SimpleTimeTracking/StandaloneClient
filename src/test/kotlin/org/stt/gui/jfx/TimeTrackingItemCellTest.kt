package org.stt.gui.jfx

import javafx.scene.text.Font
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.stt.gui.jfx.TimeTrackingItemCellWithActions.ActionsHandler
import org.stt.model.TimeTrackingItem
import org.stt.submit.SubmitSelectionManager
import org.stt.submit.SubmitStatusTracker
import java.time.LocalDateTime
import java.util.*
import java.util.function.Predicate

/**
 * Tests for [TimeTrackingItemCellWithActions].
 *
 * Verifies the action button handlers and that edit, delete, continue and stop buttons are
 * disabled for items submitted to any connector, while staying enabled for unsubmitted items.
 */
class TimeTrackingItemCellTest {

    private var sut: TimeTrackingItemCellWithActions? = null
    @Mock
    private val actionsHandler: ActionsHandler? = null
    @Mock
    private val submitStatusTracker: SubmitStatusTracker? = null
    private var fontAwesome: Font? = null

    @Before
    fun setup() {
        TestFX.installTK()
        fontAwesome = Font.loadFont(javaClass.getResourceAsStream("/fontawesome-webfont.ttf"), 0.0)
        MockitoAnnotations.initMocks(this)
        val resourceBundle = ResourceBundle
                .getBundle("org.stt.gui.Application")

        sut = TimeTrackingItemCellWithActions(fontAwesome!!, resourceBundle, Predicate { false }, actionsHandler!!, { it },
                submitStatusTracker!!,
                Mockito.mock(SubmitSelectionManager::class.java),
                { null })
    }

    private fun updateCellFor(item: TimeTrackingItem) {
        `when`(submitStatusTracker!!.isSubmitted(item)).thenReturn(false)
        sut!!.updateItem(item, false)
    }

    @Test
    fun shouldCallDeleteHandlerOnClickOnDelete() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val deleteButton = sut!!.deleteButton

        // WHEN
        deleteButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).delete(item)
    }

    @Test
    fun shouldCallContinueHandlerOnClickOnContinue() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val continueButton = sut!!.continueButton

        // WHEN
        continueButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).continueItem(item)
    }

    @Test
    fun shouldCallEditHandlerOnClickOnEdit() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        sut!!.updateItem(item, false)

        val editButton = sut!!.editButton
        // WHEN
        editButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).edit(item)
    }

    @Test
    fun shouldCallStopHandlerOnClickOnEdit() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        updateCellFor(item)

        val stopButton = sut!!.stopButton
        // WHEN
        stopButton.fire()

        // THEN
        verify<ActionsHandler>(actionsHandler).stop(item)
    }

    @Test
    fun shouldDisableAllActionButtonsForSubmittedItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())
        `when`(submitStatusTracker!!.isSubmitted(item)).thenReturn(true)

        // WHEN
        sut!!.updateItem(item, false)

        // THEN
        assertThat(sut!!.editButton.isDisable).isTrue()
        assertThat(sut!!.deleteButton.isDisable).isTrue()
        assertThat(sut!!.continueButton.isDisable).isTrue()
        assertThat(sut!!.stopButton.isDisable).isTrue()
    }

    @Test
    fun shouldEnableAllActionButtonsForUnsubmittedItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.now())

        // WHEN
        updateCellFor(item)

        // THEN
        assertThat(sut!!.editButton.isDisable).isFalse()
        assertThat(sut!!.deleteButton.isDisable).isFalse()
        assertThat(sut!!.continueButton.isDisable).isFalse()
        assertThat(sut!!.stopButton.isDisable).isFalse()
    }
}
