package org.stt.submit

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.stt.model.TimeTrackingItem
import java.io.File
import java.time.LocalDateTime

/**
 * Tests for [SubmitStatusTracker].
 *
 * Two main concerns:
 * 1. In-memory tracking (isSubmitted, markSubmitted, getSubmitFraction, requireNotSubmitted)
 * 2. Persistence (start/stop round-trip via the submit-status file)
 *
 * Verifies connector-level isolation, same-item content matching, empty-list edge cases,
 * and guard behavior when modifying already-submitted items.
 */

class SubmitStatusTrackerTest {

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private lateinit var homePath: String
    private lateinit var sut: SubmitStatusTracker

    @Before
    fun setup() {
        homePath = tempFolder.newFolder("home").absolutePath
        sut = SubmitStatusTracker(homePath)
    }

    @Test
    fun shouldReturnFalseForUnsubmittedItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        val result = sut.isSubmitted(item)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnFalseForUnsubmittedItemWithConnectorId() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        val result = sut.isSubmitted(item, "json")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldReturnTrueAfterMarkSubmitted() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.markSubmitted(item, "json")
        val result = sut.isSubmitted(item)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueForConnectorAfterMarkSubmitted() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.markSubmitted(item, "json")
        val result = sut.isSubmitted(item, "json")

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseForDifferentConnector() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.markSubmitted(item, "json")

        // WHEN
        val result = sut.isSubmitted(item, "csv")

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun shouldRecognizeSameItemByContent() {
        // GIVEN
        val item1 = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.markSubmitted(item1, "json")

        // WHEN
        val result = sut.isSubmitted(item2)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldHandleItemWithoutEnd() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0))

        // WHEN
        sut.markSubmitted(item, "json")
        val result = sut.isSubmitted(item)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldCalculateFullSubmitFraction() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.markSubmitted(item, "json")

        // WHEN
        val fraction = sut.getSubmitFraction(listOf(item), "json")

        // THEN
        assertThat(fraction).isEqualTo(1.0f)
    }

    @Test
    fun shouldCalculateZeroSubmitFraction() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        val fraction = sut.getSubmitFraction(listOf(item), "json")

        // THEN
        assertThat(fraction).isEqualTo(0.0f)
    }

    @Test
    fun shouldCalculatePartialSubmitFraction() {
        // GIVEN
        val item1 = TimeTrackingItem("a", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("b", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 13, 0))
        sut.markSubmitted(item1, "json")

        // WHEN
        val fraction = sut.getSubmitFraction(listOf(item1, item2), "json")

        // THEN
        assertThat(fraction).isEqualTo(0.5f)
    }

    @Test
    fun shouldReturnOneForEmptyList() {
        // WHEN
        val fraction = sut.getSubmitFraction(emptyList(), "json")

        // THEN
        assertThat(fraction).isEqualTo(1.0f)
    }

    @Test
    fun requireNotSubmittedShouldNotThrowForFreshItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))

        // WHEN
        sut.requireNotSubmitted(item)

        // THEN does not throw
    }

    @Test
    fun requireNotSubmittedShouldThrowForSubmittedItem() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.markSubmitted(item, "json")

        // WHEN / THEN
        assertThatThrownBy { sut.requireNotSubmitted(item) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("already submitted")
    }

    @Test
    fun shouldPersistAndReloadOnStart() {
        // GIVEN
        sut.start()
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        sut.markSubmitted(item, "json")
        sut.stop()

        // WHEN
        val reloaded = SubmitStatusTracker(homePath)
        reloaded.start()
        val result = reloaded.isSubmitted(item, "json")

        // THEN
        assertThat(result).isTrue()
        reloaded.stop()
    }

    @Test
    fun shouldCreateStatusDirectoryOnStart() {
        // WHEN
        sut.start()

        // THEN
        val statusDir = File(homePath, ".stt")
        assertThat(statusDir).exists().isDirectory()
        sut.stop()
    }

    @Test
    fun shouldHandleMultipleItemsAndConnectorsInPersistence() {
        // GIVEN
        sut.start()
        val item1 = TimeTrackingItem("a", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        val item2 = TimeTrackingItem("b", LocalDateTime.of(2020, 1, 1, 12, 0), LocalDateTime.of(2020, 1, 1, 13, 0))
        sut.markSubmitted(item1, "json")
        sut.markSubmitted(item1, "csv")
        sut.markSubmitted(item2, "json")
        sut.stop()

        // WHEN
        val reloaded = SubmitStatusTracker(homePath)
        reloaded.start()

        // THEN
        assertThat(reloaded.isSubmitted(item1, "json")).isTrue()
        assertThat(reloaded.isSubmitted(item1, "csv")).isTrue()
        assertThat(reloaded.isSubmitted(item2, "json")).isTrue()
        assertThat(reloaded.isSubmitted(item2, "csv")).isFalse()
        reloaded.stop()
    }

    @Test
    fun shouldStartCleanWhenNoStatusFileExists() {
        // WHEN
        sut.start()

        // THEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2020, 1, 1, 10, 0), LocalDateTime.of(2020, 1, 1, 11, 0))
        assertThat(sut.isSubmitted(item)).isFalse()
        sut.stop()
    }
}