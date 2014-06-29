package org.stt;

import static org.mockito.BDDMockito.given;

import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class ItemReaderTestHelper {

	public static void givenReaderReturns(ItemReader itemReader,
			TimeTrackingItem... items) {

		BDDMyOngoingStubbing<Optional<TimeTrackingItem>> stubbing = given(itemReader
				.read());
		for (TimeTrackingItem item : items) {
			stubbing = stubbing.willReturn(Optional.of(item));
		}
		stubbing.willReturn(Optional.<TimeTrackingItem> absent());
	}
}
