package org.stt.filter;

import com.google.common.base.Optional;
import org.hamcrest.collection.IsCollectionWithSize;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class StartDateReaderFilterTest {
	private StartDateReaderFilter sut;
	@Mock
	private ItemReader reader;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldContainStartInclusiveAndEndExclusive() throws IOException {
		// GIVEN
		DateTime from = new DateTime(2000, 1, 1, 0, 0);
		DateTime to = new DateTime(2000, 1, 2, 0, 0);
		givenReaderReturnsItemsFor(from, to);
		sut = new StartDateReaderFilter(reader, from, to);

		// WHEN
		Collection<TimeTrackingItem> result = IOUtil.readAll(sut);

		// THEN
		assertThat(result, IsCollectionWithSize.hasSize(1));
		assertThat(result.iterator().next().getStart(), is(from));
	}

	private void givenReaderReturnsItemsFor(DateTime... starts) {
		BDDMyOngoingStubbing<Optional<TimeTrackingItem>> stubbing = given(reader
				.read());
		for (DateTime start : starts) {
			stubbing = stubbing.willReturn(Optional.of(new TimeTrackingItem("",
					start)));
		}
		stubbing.willReturn(Optional.<TimeTrackingItem> absent());
	}

}
