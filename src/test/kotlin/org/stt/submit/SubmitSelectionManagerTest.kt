package org.stt.submit

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.stt.model.TimeTrackingItem
import java.time.LocalDateTime

/**
 * Tests for [SubmitSelectionManager].
 *
 * Verifies selection/deselection of items per connector, isolation between connectors,
 * same-item recognition by content (Base64-encoded key), and clear/get operations.
 * Single purpose: validate the in-memory selection state management.
 */

class SubmitSelectionManagerTest {

    private lateinit var sut: SubmitSelectionManager

    @Before
    fun setup() {
        sut = SubmitSelectionManager()
    }

    @Test
    fun shouldReturnFalseForUnselectedItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0))

        // WHEN
        val result = sut.isSelected("connector1", item)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueAfterSelectingItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.setSelected("connector1", item, true)

        // WHEN
        val result = sut.isSelected("connector1", item)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseAfterDeselectingItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.setSelected("connector1", item, true)

        // WHEN
        sut.setSelected("connector1", item, false)
        val result = sut.isSelected("connector1", item)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldIsolateSelectionsByConnectorId() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.setSelected("connector1", item, true)

        // WHEN
        val resultOther = sut.isSelected("connector2", item)
        val resultOriginal = sut.isSelected("connector1", item)

        // THEN
        assertThat(resultOther).isFalse()
        assertThat(resultOriginal).isTrue()
    }

    @Test
    fun shouldRecognizeSameItemByContent() {
        // GIVEN
        val item1 = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.setSelected("connector1", item1, true)

        // WHEN
        val result = sut.isSelected("connector1", item2)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldGetSelectedItemsReturnsKeys() {
        // GIVEN
        val item1 = TimeTrackingItem("a", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("b", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 13, 0))
        sut.setSelected("connector1", item1, true)
        sut.setSelected("connector1", item2, true)

        // WHEN
        val keys = sut.getSelectedItems("connector1")

        // THEN
        assertThat(keys).hasSize(2)
    }

    @Test
    fun shouldReturnEmptySetForUnknownConnector() {
        // WHEN
        val result = sut.getSelectedItems("nonexistent")

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun shouldClearAllSelectionsForConnector() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.setSelected("connector1", item, true)

        // WHEN
        sut.clearSelection("connector1")
        val result = sut.isSelected("connector1", item)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldHandleItemWithoutEnd() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0))

        // WHEN
        sut.setSelected("connector1", item, true)
        val result = sut.isSelected("connector1", item)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldNotAffectOtherConnectorsOnClear() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.setSelected("connector1", item, true)
        sut.setSelected("connector2", item, true)

        // WHEN
        sut.clearSelection("connector1")
        val result = sut.isSelected("connector2", item)

        // THEN
        assertThat(result).isTrue()
    }
}