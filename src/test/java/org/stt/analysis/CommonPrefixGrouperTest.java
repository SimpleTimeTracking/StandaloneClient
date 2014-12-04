package org.stt.analysis;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.ItemReaderTestHelper;
import org.stt.analysis.CommonPrefixGrouper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

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
		givenReaderReturnsItemsWithComment(firstComment, "group subgroup two");
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
		givenReaderReturnsItemsWithComment(firstComment, "group subgroup two",
				thirdComment);
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
		givenReaderReturnsItemsWithComment(firstComment, "group two");
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
		givenReaderReturnsItemsWithComment(firstComment, firstComment);
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(groups, is(Arrays.asList(firstComment)));
	}

	@Test
	public void shouldCutGroupAtShorterItem()
	{
		// GIVEN
		sut.learnLine("aaaa");
		sut.learnLine("aaaa bbbb");
		sut.learnLine("aaaa bbbb cccc");

		// WHEN
		List<String> result = sut.getGroupsOf("aaaa bbbb cccc dddd");

		// THEN
		assertThat(result, is(Arrays.asList("aaaa", "bbbb", "cccc", "dddd")));
	}

	private TimeTrackingItem[] givenReaderReturnsItemsWithComment(
			String... comments) {
		TimeTrackingItem items[] = new TimeTrackingItem[comments.length];
		for (int i = 0; i < comments.length; i++) {
			items[i] = new TimeTrackingItem(comments[i], DateTime.now());
		}
		ItemReaderTestHelper.givenReaderReturns(itemReader, items);
		return items;
	}

}
