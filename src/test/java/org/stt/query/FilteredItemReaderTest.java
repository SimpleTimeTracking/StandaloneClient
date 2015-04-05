package org.stt.query;

import com.google.common.base.Optional;
import org.hamcrest.collection.IsCollectionWithSize;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class FilteredItemReaderTest {
	private FilteredItemReader sut;
	@Mock
	private ItemReader reader;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldFilterUsingQuery() throws IOException {
		// GIVEN
		DateTime from = new DateTime(2000, 1, 1, 0, 0);
		DateTime to = new DateTime(2000, 1, 2, 0, 0);
		givenReaderReturnsItemsFor(from, to);
		DNFClause dnfClause = new DNFClause();
		dnfClause.withStartBetween(new Interval(from, to));
		sut = new FilteredItemReader(reader, dnfClause);

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

	@Test
	public void itemContainingSubstringGetsFound() throws IOException {

		// GIVEN
		ItemReader readerMock = Mockito.mock(ItemReader.class);
		ItemReaderTestHelper.givenReaderReturns(readerMock,
				new TimeTrackingItem("the comment", DateTime.now()));

		DNFClause dnfClause = new DNFClause();
		dnfClause.withCommentContains("comment");
		FilteredItemReader filter = new FilteredItemReader(readerMock, dnfClause);

		// WHEN
		Optional<TimeTrackingItem> read = filter.read();
		filter.close();

		// THEN
		Assert.assertEquals("the comment", read.get().getComment().get());
	}

	@Test
	public void itemNotContainingSubstringGetsNotFound() throws IOException {

		// GIVEN
		ItemReader readerMock = Mockito.mock(ItemReader.class);
		ItemReaderTestHelper.givenReaderReturns(readerMock,
				new TimeTrackingItem("the comment", DateTime.now()));

		DNFClause dnfClause = new DNFClause();
		dnfClause.withCommentContains("not there");
		FilteredItemReader filter = new FilteredItemReader(readerMock, dnfClause);

		// WHEN
		Optional<TimeTrackingItem> read = filter.read();
		filter.close();

		// THEN
		Assert.assertEquals(Optional.<TimeTrackingItem>absent(), read);
	}

	@Test
	public void nullSearchStringReturnsAllItems() throws IOException {

		// GIVEN
		ItemReader readerMock = Mockito.mock(ItemReader.class);
		ItemReaderTestHelper.givenReaderReturns(readerMock,
				new TimeTrackingItem("the comment", DateTime.now()));

		DNFClause dnfClause = new DNFClause();
		FilteredItemReader filter = new FilteredItemReader(readerMock, dnfClause);

		// WHEN
		Optional<TimeTrackingItem> read = filter.read();
		filter.close();

		// THEN
		Assert.assertEquals("the comment", read.get().getComment().get());
	}
}
