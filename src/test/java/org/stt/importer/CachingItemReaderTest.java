package org.stt.importer;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.stt.CachingItemReader;

import com.google.common.base.Optional;

import static org.hamcrest.Matchers.is;

public class CachingItemReaderTest {

	@Test
	public void cachedItemsCanBeReadMultipleTimes() {
		// GIVEN
		TimeTrackingItem expected1 = new TimeTrackingItem("first",
				DateTime.now());
		TimeTrackingItem expected2 = new TimeTrackingItem("second",
				DateTime.now());
		ItemReader readerMock = Mockito.mock(ItemReader.class);
		ItemReaderTestHelper.givenReaderReturns(readerMock, expected1,
				expected2);
		CachingItemReader cacher = new CachingItemReader(readerMock);

		// WHEN
		// read all items, so they are cached
		while (cacher.read().isPresent()) { // NOPMD

		}

		// THEN
		Assert.assertThat(cacher.read().get(), is(expected1));
		Assert.assertThat(cacher.read().get(), is(expected2));
		Assert.assertThat(cacher.read(),
				is(Optional.<TimeTrackingItem> absent()));
		Assert.assertThat(cacher.read().get(), is(expected1));

		IOUtils.closeQuietly(cacher);
	}
}
