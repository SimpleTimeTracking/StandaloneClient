package org.stt.filter;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.query.SubstringReaderFilter;

import java.io.IOException;

public class SubstringReaderFilterTest {

	@Test
	public void itemContainingSubstringGetsFound() throws IOException {

		// GIVEN
		ItemReader readerMock = Mockito.mock(ItemReader.class);
		ItemReaderTestHelper.givenReaderReturns(readerMock,
				new TimeTrackingItem("the comment", DateTime.now()));

		SubstringReaderFilter filter = new SubstringReaderFilter(readerMock,
				"comment");

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

		SubstringReaderFilter filter = new SubstringReaderFilter(readerMock,
				"not there");

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

		SubstringReaderFilter filter = new SubstringReaderFilter(readerMock,
				null);

		// WHEN
		Optional<TimeTrackingItem> read = filter.read();
		filter.close();

		// THEN
		Assert.assertEquals("the comment", read.get().getComment().get());
	}
}
