package org.stt.reporting;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class CommonPrefixGrouperTest {
	private final CommonPrefixGrouper sut = new CommonPrefixGrouper();

	@Mock
	private ItemReader itemReader;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldFindGroupsWithSpaces() {
		// GIVEN
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				"group subgroup one", "group subgroup two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> result = sut.getGroupsOf(items[0]);

		// THEN
		assertThat(result, is(Arrays.asList("group subgroup", "one")));

	}

	@Test
	public void shouldFindSubGroups() {
		// GIVEN
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				"group subgroup one", "group subgroup two",
				"group subgroup2 one");
		sut.scanForGroups(itemReader);
		System.out.println(sut.toString());

		// WHEN
		List<String> withThreeGroups = sut.getGroupsOf(items[0]);
		List<String> withTwoGroups = sut.getGroupsOf(items[2]);

		// THEN
		assertThat(withThreeGroups,
				is(Arrays.asList("group", "subgroup", "one")));
		assertThat(withTwoGroups, is(Arrays.asList("group", "subgroup2 one")));
	}

	@Test
	public void shouldFindLongestCommonPrefix() {
		// GIVEN
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				"group one", "group two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(items[0]);

		// THEN
		assertThat(groups, is(Arrays.asList("group", "one")));

	}

	@Test
	public void shouldFindGroups() {
		// GIVEN
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment("group",
				"group");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(items[0]);

		// THEN
		assertThat(groups, is(Arrays.asList("group")));
	}

	private TimeTrackingItem[] givenReaderReturnsItemsWithComment(
			String... comments) {
		TimeTrackingItem items[] = new TimeTrackingItem[comments.length];
		for (int i = 0; i < comments.length; i++) {
			items[i] = new TimeTrackingItem(comments[i], DateTime.now());
		}
		givenReaderReturns(items);
		return items;
	}

	private void givenReaderReturns(TimeTrackingItem... items) {

		BDDMyOngoingStubbing<Optional<TimeTrackingItem>> stubbing = given(itemReader
				.read());
		for (TimeTrackingItem item : items) {
			stubbing = stubbing.willReturn(Optional.of(item));
		}
		stubbing.willReturn(Optional.<TimeTrackingItem> absent());
	}

}
