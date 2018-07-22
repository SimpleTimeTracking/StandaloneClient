package org.stt.text

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.stt.config.ConfigRoot
import org.stt.model.TimeTrackingItem
import org.stt.query.TimeTrackingItemQueries
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

class CommonPrefixGrouperTest {
    @Mock
    private lateinit var queries: TimeTrackingItemQueries
    private lateinit var sut: CommonPrefixGrouper

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        sut = CommonPrefixGrouper(queries, ConfigRoot().prefixGrouper)
    }

    @Test
    fun shouldFindExpansion() {
        // GIVEN
        givenReaderReturnsItemsWithComment("group subgroup one",
                "group subgroup two")

        // WHEN
        val expansions = sut.getPossibleExpansions("gr")

        // THEN
        assertThat(expansions, `is`(listOf("oup subgroup ")))
    }

    @Test
    fun shouldFindSubgroupExpansion() {
        // GIVEN
        givenReaderReturnsItemsWithComment("group subgroup one",
                "group subgroup two")

        // WHEN
        val expansions = sut.getPossibleExpansions("group subgroup o")

        // THEN
        assertThat(expansions, `is`(listOf("ne")))
    }

    @Test
    fun shouldFindMultipleExpansions() {
        givenReaderReturnsItemsWithComment("group subgroup one",
                "group subgroup two")

        // WHEN
        val expansions = sut.getPossibleExpansions("group subgroup ")

        // THEN
        assertThat(expansions, hasItems("two", "one"))
    }

    @Test
    fun shouldHandleGroupsWithLongerTextThanGivenComment() {
        // GIVEN
        givenReaderReturnsItemsWithComment("test")

        // WHEN
        val result = groupsAsString("t")

        // THEN
        assertThat(result, `is`(listOf("t")))
    }

    private fun groupsAsString(t: String): List<String> {
        return sut(t).stream().map { g -> g.content }.toList()
    }

    @Test
    fun shouldFindGroupsWithSpaces() {
        // GIVEN
        val firstComment = "group subgroup one"
        givenReaderReturnsItemsWithComment(firstComment, "group subgroup two")

        // WHEN
        val result = groupsAsString(firstComment)

        // THEN
        assertThat(result, `is`(Arrays.asList("group subgroup", "one")))

    }

    @Test
    fun shouldFindSubGroups() {
        // GIVEN
        val firstComment = "group subgroup one"
        val thirdComment = "group subgroup2 one"
        givenReaderReturnsItemsWithComment(firstComment, "group subgroup two",
                thirdComment)

        // WHEN
        val withThreeGroups = groupsAsString(firstComment)
        val withTwoGroups = groupsAsString(thirdComment)

        // THEN
        assertThat(withThreeGroups,
                `is`(Arrays.asList("group subgroup", "one")))
        assertThat(withTwoGroups, `is`(Arrays.asList("group subgroup2", "one")))
    }

    @Test
    fun shouldFindLongestCommonPrefix() {
        // GIVEN
        val firstComment = "group one"
        givenReaderReturnsItemsWithComment(firstComment, "group two")

        // WHEN
        val groups = groupsAsString(firstComment)

        // THEN
        assertThat(groups, `is`(Arrays.asList("group", "one")))

    }

    @Test
    fun shouldFindGroups() {
        // GIVEN
        val firstComment = "group"
        givenReaderReturnsItemsWithComment(firstComment, firstComment)

        // WHEN
        val groups = groupsAsString(firstComment)

        // THEN
        assertThat(groups, `is`(listOf(firstComment)))
    }

    @Test
    fun shouldCutGroupAtShorterItem() {
        // GIVEN
        givenReaderReturnsItemsWithComment()
        sut.insert("aaaa")
        sut.insert("aaaa bbbb")
        sut.insert("aaaa bbbb cccc")

        // WHEN
        val result = groupsAsString("aaaa bbbb cccc dddd")

        // THEN
        assertThat(result, `is`(Arrays.asList("aaaa", "bbbb", "cccc", "dddd")))
    }

    private fun givenReaderReturnsItemsWithComment(
            vararg comments: String) {
        val items = arrayOfNulls<TimeTrackingItem>(comments.size)
        for (i in comments.indices) {
            items[i] = TimeTrackingItem(comments[i], LocalDateTime.now())
        }
        given(queries.queryAllItems()).willReturn(Stream.of(*items))
    }

}
