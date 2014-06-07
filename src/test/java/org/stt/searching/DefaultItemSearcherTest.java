package org.stt.searching;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemSearcher;

import com.google.common.base.Optional;

public class DefaultItemSearcherTest {

	@SuppressWarnings("unchecked")
	@Test
	public void searchByExactStartTime() {

		// GIVEN
		DateTime expected = DateTime.now();
		TimeTrackingItem expectedItem = new TimeTrackingItem(
				"the expected one", expected);

		ItemReader dummyReader = Mockito.mock(ItemReader.class);
		Mockito.when(dummyReader.read()).thenReturn(
				Optional.of(expectedItem),
				Optional.of(new TimeTrackingItem("", DateTime.now()
						.minus(10000))), Optional.<TimeTrackingItem> absent());

		ItemSearcher searcher = new DefaultItemSearcher(dummyReader);

		// WHEN
		Collection<TimeTrackingItem> foundElements = searcher.searchByStart(
				expected, expected);

		// THEN
		Assert.assertThat(foundElements, Matchers.hasItem(expectedItem));
		Assert.assertThat(foundElements, Matchers.hasSize(1));
	}

}
