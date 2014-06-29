package org.stt.reporting;

import static org.hamcrest.CoreMatchers.hasItems;
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
	public void shouldFindExpansion() {
		// GIVEN
		givenReaderReturnsItemsWithComment("group subgroup one",
				"group subgroup two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> expansions = sut.getPossibleExpansions("gr");

		// THEN
		assertThat(expansions, is(Arrays.asList("oup subgroup")));
	}

	@Test
	public void shouldFindSubgroupExpansion() {
		// GIVEN
		givenReaderReturnsItemsWithComment("group subgroup one",
				"group subgroup two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> expansions = sut.getPossibleExpansions("group subgroup o");

		// THEN
		assertThat(expansions, is(Arrays.asList("ne")));
	}

	@Test
	public void shouldFindMultipleExpansions() {
		givenReaderReturnsItemsWithComment("group subgroup one",
				"group subgroup two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> expansions = sut.getPossibleExpansions("group subgroup ");

		// THEN
		assertThat(expansions, hasItems("two", "one"));
	}

	@Test
	public void shouldHandleGroupsWithLongerTextThanGivenComment() {
		// GIVEN
		givenReaderReturnsItemsWithComment("test");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> result = sut.getGroupsOf("t");

		// THEN
		assertThat(result, is(Arrays.asList("t")));
	}

	@Test
	public void shouldFindGroupsWithSpaces() {
		// GIVEN
		String firstComment = "group subgroup one";
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				firstComment, "group subgroup two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> result = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(result, is(Arrays.asList("group subgroup", "one")));

	}

	@Test
	public void shouldFindSubGroups() {
		// GIVEN
		String firstComment = "group subgroup one";
		String thirdComment = "group subgroup2 one";
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				firstComment, "group subgroup two", thirdComment);
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> withThreeGroups = sut.getGroupsOf(firstComment);
		List<String> withTwoGroups = sut.getGroupsOf(thirdComment);

		// THEN
		assertThat(withThreeGroups,
				is(Arrays.asList("group", "subgroup", "one")));
		assertThat(withTwoGroups, is(Arrays.asList("group", "subgroup2 one")));
	}

	@Test
	public void shouldFindLongestCommonPrefix() {
		// GIVEN
		String firstComment = "group one";
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				firstComment, "group two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(groups, is(Arrays.asList("group", "one")));

	}

	@Test
	public void shouldFindGroups() {
		// GIVEN
		String firstComment = "group";
		TimeTrackingItem[] items = givenReaderReturnsItemsWithComment(
				firstComment, firstComment);
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(groups, is(Arrays.asList(firstComment)));
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
