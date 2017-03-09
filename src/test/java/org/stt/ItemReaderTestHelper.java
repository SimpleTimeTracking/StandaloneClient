package org.stt;

import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.util.Optional;

import static org.mockito.BDDMockito.given;

public class ItemReaderTestHelper {

	public static void givenReaderReturns(ItemReader itemReader,
			TimeTrackingItem... items) {

		BDDMyOngoingStubbing<Optional<TimeTrackingItem>> stubbing = given(itemReader
				.read());
		for (TimeTrackingItem item : items) {
			stubbing = stubbing.willReturn(Optional.of(item));
		}
        stubbing.willReturn(Optional.empty());
    }
}
