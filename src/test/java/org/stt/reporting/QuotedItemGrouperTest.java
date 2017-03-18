package org.stt.reporting;

import org.junit.Test;
import org.stt.text.QuotedItemGrouper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QuotedItemGrouperTest {
	private final QuotedItemGrouper sut = new QuotedItemGrouper();

	@Test
	public void shouldMatchSimpleGroup() {
		// GIVEN

		// WHEN
		List<String> result = sut.getGroupsOf("'test'");

		// THEN
        assertThat(result, is(Collections.singletonList("test")));
    }

	@Test
	public void shouldMatchCompleteStringIfNoDelimitersAreDetected() {
		// GIVEN

		// WHEN
		String completeText = "test with complete string";
		List<String> result = sut.getGroupsOf(completeText);

		// THEN
        assertThat(result, is(Collections.singletonList(completeText)));
    }

	@Test
	public void shouldMatchMixedDelimitersAndSubgroups() {
		// GIVEN

		// WHEN
		String completeText = "'group' %sub group% &child group& rest of text";
		List<String> result = sut.getGroupsOf(completeText);

		// THEN
		assertThat(result, is(Arrays.asList("group", "sub group",
				"child group", "rest of text")));
	}
}
