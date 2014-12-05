package org.stt;

import com.google.common.base.Optional;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import static org.mockito.BDDMockito.given;

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
