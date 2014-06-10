package org.stt.searching;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemSearcher;

import com.google.common.base.Optional;

public class DefaultItemSearcherTest {

	@Mock
	private ItemReader reader;
	private ItemReaderProvider provider;
	private ItemSearcher sut;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		provider = new ItemReaderProvider() {

			@Override
			public ItemReader provideReader() {
				return reader;
			}
		};
		sut = new DefaultItemSearcher(provider);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void searchByExactStartTime() {

		// GIVEN
		DateTime expected = DateTime.now();
		TimeTrackingItem expectedItem = new TimeTrackingItem(
				"the expected one", expected);

		Mockito.when(reader.read()).thenReturn(
				Optional.of(expectedItem),
				Optional.of(new TimeTrackingItem("", DateTime.now()
						.minus(10000))), Optional.<TimeTrackingItem> absent());

		// WHEN
		Collection<TimeTrackingItem> foundElements = sut.searchByStart(
				expected, expected);

		// THEN
		assertThat(foundElements, Matchers.hasItem(expectedItem));
		assertThat(foundElements, Matchers.hasSize(1));
	}

	@Test
	public void shouldNotFindCurrentItemIfNoneCanBeRead() {
		// GIVEN
		given(reader.read()).willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result, is(Optional.<TimeTrackingItem> absent()));
	}

	@Test
	public void shouldFindCurrentItem() {
		// GIVEN
		TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
				DateTime.now());
		given(reader.read()).willReturn(Optional.of(unfinishedItem))
				.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result.get(), is(unfinishedItem));
	}

	@Test
	public void shouldNotFindCurrentItemIfLastOneIsFinished() {
		// GIVEN
		TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
				DateTime.now(), DateTime.now().plusMillis(1));
		given(reader.read()).willReturn(Optional.of(unfinishedItem))
				.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result, is(Optional.<TimeTrackingItem> absent()));
	}

	@Test
	public void shouldOnlyFindCurrentItem() {
		// GIVEN
		TimeTrackingItem finishedItem = new TimeTrackingItem(null,
				DateTime.now(), DateTime.now().plusMillis(1));
		TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
				DateTime.now());
		given(reader.read()).willReturn(Optional.of(finishedItem))
				.willReturn(Optional.of(unfinishedItem))
				.willReturn(Optional.<TimeTrackingItem> absent());

		// WHEN
		Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

		// THEN
		assertThat(result.get(), is(unfinishedItem));
	}

}
