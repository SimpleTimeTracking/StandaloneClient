package org.stt.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.stt.query.TimeTrackingItemQueries
import java.time.LocalDateTime
import java.util.stream.Stream

class ItemAndDateValidatorTest {
    @Mock
    private lateinit var timeTrackingItemQueries: TimeTrackingItemQueries

    private lateinit var sut: ItemAndDateValidator

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = ItemAndDateValidator(timeTrackingItemQueries)
    }

    @Test
    fun shouldReturnTrueForNonTodayDate() {
        // GIVEN
        val yesterday = LocalDateTime.now().minusDays(1)

        // WHEN
        val result = sut.validateItemIsFirstItemAndLater(yesterday)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueWhenEarlierItemExistsOnSameDay() {
        // GIVEN
        val now = LocalDateTime.now()
        given(timeTrackingItemQueries.queryItems(any())).willReturn(Stream.of(
                org.stt.model.TimeTrackingItem("test", now.minusHours(1))))

        // WHEN
        val result = sut.validateItemIsFirstItemAndLater(now)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnTrueWhenTimeIsNotInTheFuture() {
        // GIVEN
        val now = LocalDateTime.now()
        given(timeTrackingItemQueries.queryItems(any())).willReturn(Stream.empty())

        // WHEN
        val result = sut.validateItemIsFirstItemAndLater(now)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun shouldReturnFalseWhenTimeIsInFutureAndNoEarlierItem() {
        // GIVEN
        val future = LocalDateTime.now().plusHours(1)
        given(timeTrackingItemQueries.queryItems(any())).willReturn(Stream.empty())

        // WHEN
        val result = sut.validateItemIsFirstItemAndLater(future)

        // THEN
        assertThat(result).isFalse()
    }
}