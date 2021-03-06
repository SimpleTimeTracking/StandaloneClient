package org.stt.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.experimental.theories.suppliers.TestedOn
import org.junit.runner.RunWith
import org.mockito.BDDMockito.BDDMyOngoingStubbing
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.stt.importer.IOUtil
import org.stt.model.TimeTrackingItem
import java.time.LocalDateTime
import java.util.*

@RunWith(Theories::class)
class IOUtilTest {

    @Theory
    fun readAllShouldReadAll(
            @TestedOn(ints = intArrayOf(0, 1, 10, 100)) numberOfItems: Int) {
        // GIVEN

        val itemReader = mock(ItemReader::class.java)
        val expectedItems = ArrayList<TimeTrackingItem>()
        var stubbing: BDDMyOngoingStubbing<TimeTrackingItem?> = given(itemReader
                .read())
        for (i in 0 until numberOfItems) {
            val item = TimeTrackingItem(i.toString(), LocalDateTime.now())
            expectedItems.add(item)
            stubbing = stubbing.willReturn(item)
        }
        stubbing.willReturn(null)

        // WHEN
        val result = IOUtil.readAll(itemReader)

        // THEN
        assertThat(result).isEqualTo(expectedItems)
        verify(itemReader).close()
    }
}
