package org.stt

import org.mockito.BDDMockito.BDDMyOngoingStubbing
import org.mockito.BDDMockito.given
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader

object ItemReaderTestHelper {

    fun givenReaderReturns(itemReader: ItemReader,
                           vararg items: TimeTrackingItem) {

        var stubbing: BDDMyOngoingStubbing<TimeTrackingItem> = given(itemReader
                .read())
        for (item in items) {
            stubbing = stubbing.willReturn(item)
        }
        stubbing.willReturn(null)
    }
}
