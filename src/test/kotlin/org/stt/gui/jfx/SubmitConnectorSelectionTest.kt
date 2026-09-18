package org.stt.gui.jfx

import javafx.scene.control.ComboBox
import javafx.scene.text.Font
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.stt.config.ConfigRoot
import org.stt.config.ConfigServiceFacade
import org.stt.model.TimeTrackingItem
import org.stt.submit.SubmitConfig
import org.stt.submit.SubmitConnector
import org.stt.submit.SubmitStatusTracker
import java.time.LocalDateTime

/**
 * Verifies the shared submit connector selection helpers: restoring the
 * persisted last-used connector into the views' ComboBoxes (including the
 * fallback to the first connector), persisting the selection, and computing
 * the fully-submitted (immutable) state of an item group.
 */
class SubmitConnectorSelectionTest {

    private lateinit var tempoConnector: SubmitConnector
    private lateinit var jsonConnector: SubmitConnector
    private lateinit var comboBox: ComboBox<SubmitConnector>
    private val configRoot = ConfigRoot()

    @Before
    fun setup() {
        TestFX.installTK()
        Font.getDefault()
        tempoConnector = Mockito.mock(SubmitConnector::class.java)
        given(tempoConnector.id).willReturn("tempo")
        jsonConnector = Mockito.mock(SubmitConnector::class.java)
        given(jsonConnector.id).willReturn("json")
        comboBox = ComboBox<SubmitConnector>().apply { items.addAll(jsonConnector, tempoConnector) }
    }

    @Test
    fun shouldSelectStoredConnectorWhenKnown() {
        // GIVEN
        configRoot.submit = SubmitConfig(lastUsedConnectorId = "tempo")

        // WHEN
        selectLastUsedConnector(comboBox, comboBox.items, configRoot)

        // THEN
        assertThat(comboBox.selectionModel.selectedItem).isSameAs(tempoConnector)
    }

    @Test
    fun shouldFallBackToFirstConnectorWhenStoredIdUnknown() {
        // GIVEN
        configRoot.submit = SubmitConfig(lastUsedConnectorId = "deletedConnector")

        // WHEN
        selectLastUsedConnector(comboBox, comboBox.items, configRoot)

        // THEN
        assertThat(comboBox.selectionModel.selectedItem).isSameAs(jsonConnector)
    }

    @Test
    fun shouldFallBackToFirstConnectorWhenNothingStored() {
        // GIVEN
        configRoot.submit = SubmitConfig(lastUsedConnectorId = null)

        // WHEN
        selectLastUsedConnector(comboBox, comboBox.items, configRoot)

        // THEN
        assertThat(comboBox.selectionModel.selectedItem).isSameAs(jsonConnector)
    }

    @Test
    fun shouldPersistSelectedConnectorIdAndSaveImmediately() {
        // GIVEN
        val configService = Mockito.mock(ConfigServiceFacade::class.java)
        given(configService.config).willReturn(configRoot)

        // WHEN
        persistLastUsedConnector(configService, tempoConnector)

        // THEN
        assertThat(configRoot.submit.lastUsedConnectorId).isEqualTo("tempo")
        Mockito.verify(configService).save()
    }

    @Test
    fun shouldNotPersistWhenNoConnectorSelected() {
        // GIVEN
        configRoot.submit = SubmitConfig(lastUsedConnectorId = "tempo")
        val configService = Mockito.mock(ConfigServiceFacade::class.java)
        given(configService.config).willReturn(configRoot)

        // WHEN
        persistLastUsedConnector(configService, null)

        // THEN
        assertThat(configRoot.submit.lastUsedConnectorId).isEqualTo("tempo")
        Mockito.verify(configService, Mockito.never()).save()
    }

    @Test
    fun shouldSelectLocalConnectorMatchingByIdEvenWhenInstanceDiffers() {
        // GIVEN - another instance with the same id, as connectors are not shared between views
        val otherTempoInstance = Mockito.mock(SubmitConnector::class.java)
        given(otherTempoInstance.id).willReturn("tempo")

        // WHEN
        selectConnectorById(comboBox, comboBox.items, otherTempoInstance.id)

        // THEN - the local tempo connector is selected instead of clearing the selection
        assertThat(comboBox.selectionModel.selectedItem).isSameAs(tempoConnector)
    }

    @Test
    fun shouldNotChangeSelectionWhenIdUnknown() {
        // GIVEN
        comboBox.selectionModel.select(tempoConnector)

        // WHEN
        selectConnectorById(comboBox, comboBox.items, "deletedConnector")

        // THEN
        assertThat(comboBox.selectionModel.selectedItem).isSameAs(tempoConnector)
    }

    @Test
    fun shouldNotReportFullySubmittedForEmptyItems() {
        // GIVEN
        val tracker = Mockito.mock(SubmitStatusTracker::class.java)

        // WHEN
        val result = isFullySubmitted(emptyList(), tracker)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReportFullySubmittedWhenAllItemsSubmitted() {
        // GIVEN
        val tracker = Mockito.mock(SubmitStatusTracker::class.java)
        val item1 = TimeTrackingItem("a", LocalDateTime.now())
        val item2 = TimeTrackingItem("b", LocalDateTime.now())
        given(tracker.isSubmitted(item1)).willReturn(true)
        given(tracker.isSubmitted(item2)).willReturn(true)

        // WHEN
        val result = isFullySubmitted(listOf(item1, item2), tracker)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldNotReportFullySubmittedWhenSomeItemsSubmitted() {
        // GIVEN
        val tracker = Mockito.mock(SubmitStatusTracker::class.java)
        val item1 = TimeTrackingItem("a", LocalDateTime.now())
        val item2 = TimeTrackingItem("b", LocalDateTime.now())
        given(tracker.isSubmitted(item1)).willReturn(true)
        given(tracker.isSubmitted(item2)).willReturn(false)

        // WHEN
        val result = isFullySubmitted(listOf(item1, item2), tracker)

        // THEN
        assertThat(result).isFalse()
    }
}
