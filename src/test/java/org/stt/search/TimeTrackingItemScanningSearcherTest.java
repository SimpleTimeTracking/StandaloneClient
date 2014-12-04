package org.stt.search;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Collection;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

import com.google.common.base.Optional;

public class TimeTrackingItemScanningSearcherTest {
	private TimeTrackingItemScanningSearcher sut;

	@Mock
	protected ItemReader itemReader;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		ItemReaderProvider itemReaderProvider = new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				return itemReader;
			}
		};
		sut = new TimeTrackingItemScanningSearcher(itemReaderProvider);
	}

	@Test
	public void shouldReturnFullCommentForPartialMatch() {
		// GIVEN
		givenItemReaderReturnsComments("test test");

		// WHEN
		Collection<String> result = sut.searchForComments("test");

		// THEN
		assertThat(result, is((Collection<String>) Arrays.asList("test test")));
	}

	private void givenItemReaderReturnsComments(String... comments) {
		BDDMyOngoingStubbing<Optional<TimeTrackingItem>> stubbing = given(itemReader
				.read());
		for (String comment : comments) {
			stubbing = stubbing.willReturn(Optional.of(new TimeTrackingItem(
					comment, DateTime.now())));

		}
		stubbing.willReturn(Optional.<TimeTrackingItem> absent());

	}

}
