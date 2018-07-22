package org.stt.command

import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers
import org.hamcrest.collection.IsCollectionWithSize
import org.junit.Assert.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.experimental.theories.suppliers.TestedOn
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemReader
import org.stt.persistence.stt.STTItemPersister
import org.stt.persistence.stt.STTItemReader
import org.stt.query.TimeTrackingItemQueries
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.ChronoLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Provider
import kotlin.streams.toList

@RunWith(Theories::class)
class CommandFormatterTest {
    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private lateinit var sut: CommandFormatter
    private lateinit var timeTrackingItemQueries: TimeTrackingItemQueries

    private lateinit var itemWriter: STTItemPersister
    private lateinit var activities: Activities

    @Before
    @Throws(IOException::class)
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val tempFile = tempFolder.newFile()
        val readerSupplier = Provider {
            InputStreamReader(FileInputStream(tempFile), StandardCharsets.UTF_8)
        }
        val itemReaderProvider = Provider<ItemReader> { STTItemReader(readerSupplier.get()) }
        itemWriter = STTItemPersister(Provider { readerSupplier.get() },
                Provider { OutputStreamWriter(FileOutputStream(tempFile), StandardCharsets.UTF_8) })
        timeTrackingItemQueries = TimeTrackingItemQueries(itemReaderProvider, Optional.empty())
        activities = Activities(itemWriter, timeTrackingItemQueries, Optional.empty())
        sut = CommandFormatter(CommandTextParser(TIME_FORMATTER,
                DATE_TIME_FORMATTER), DATE_TIME_FORMATTER, TIME_FORMATTER)
    }


    @Test
    fun itemToCommandShouldUseSinceIfEndIsMissing() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2000,
                1, 1, 1, 1, 1))

        // WHEN
        val result = sut.asNewItemCommandText(item)

        // THEN
        assertThat(result, `is`("test since 2000.1.1 1:1:1"))
    }

    @Test
    fun itemToCommandShouldUseFromToIfEndIsNotMissing() {
        // GIVEN
        val item = TimeTrackingItem("test", LocalDateTime.of(2000,
                1, 1, 1, 1, 1), LocalDateTime.of(2000, 1, 1, 1, 1, 1))

        // WHEN
        val result = sut.asNewItemCommandText(item)

        // THEN
        assertThat(result,
                `is`("test from 2000.1.1 1:1:1 to 2000.1.1 1:1:1"))
    }

    @Test
    fun itemToCommandShouldUseLongFormatIfEndIsTomorrow() {
        // GIVEN
        val expectedStart = LocalDateTime.now()
        val expectedEnd = LocalDateTime.now().plusDays(1)
        val item = TimeTrackingItem("test", expectedStart,
                expectedEnd)

        // WHEN
        val result = sut.asNewItemCommandText(item)

        // THEN
        val startString = TIME_FORMATTER.format(expectedStart)
        val endString = DATE_TIME_FORMATTER.format(expectedEnd)
        assertThat(result, `is`("test from $startString to $endString"))
    }

    @Test
    fun shouldParseSince7_00() {
        // GIVEN

        // WHEN
        executeCommand("test since 7:00")

        // THEN
        val (_, start) = retrieveWrittenTimeTrackingItem()
        assertThatTimeIsTodayWith(start, 7, 0, 0)
    }

    @Test
    fun shouldParseAt7_00() {
        // GIVEN

        // WHEN
        executeCommand("test at 7:00")

        // THEN
        val (_, start) = retrieveWrittenTimeTrackingItem()
        assertThatTimeIsTodayWith(start, 7, 0, 0)
    }

    @Test
    fun shouldParseSince03_12_11() {
        // GIVEN

        // WHEN
        executeCommand("test since 03:12:11")

        // THEN
        val (_, start) = retrieveWrittenTimeTrackingItem()
        assertThatTimeIsTodayWith(start, 3, 12, 11)
    }

    @Test
    fun shouldParseSince13_37() {
        // GIVEN

        // WHEN
        executeCommand("test since 13:37")

        // THEN
        val (_, start) = retrieveWrittenTimeTrackingItem()
        assertThatTimeIsTodayWith(start, 13, 37, 0)
    }

    @Test
    fun shouldParseSince2000_01_01_13_37() {
        // GIVEN

        // WHEN
        executeCommand("test since 2000.01.01 13:37:00")

        // THEN
        val (_, start) = retrieveWrittenTimeTrackingItem()
        assertThat(start, `is`(LocalDateTime.of(2000, 1, 1, 13, 37, 0)))
    }

    private fun assertThatTimeIsTodayWith(time: LocalDateTime, hourOfDay: Int,
                                          minuteOfHour: Int, secondOfMinute: Int) {
        assertThat(time.hour, `is`(hourOfDay))
        assertThat(time.minute, `is`(minuteOfHour))
        assertThat(time.second, `is`(secondOfMinute))
        assertThat(time.get(ChronoField.MILLI_OF_SECOND), `is`(0))
        assertThat(time.toLocalDate(), `is`(LocalDate.now()))
    }

    @Test
    fun shouldParseFromXtoYCommand() {

        // GIVEN
        val expectedStart = LocalDate.now().atStartOfDay()
                .withHour(12)
        val expectedEnd = LocalDate.now().atStartOfDay()
                .withHour(13)
        val expectedItem = TimeTrackingItem("comment",
                expectedStart, expectedEnd)
        // WHEN
        val result = executeCommand("comment from 12:00 to 13:00")

        // THEN
        assertThat(result, `is`(Optional.of(expectedItem)))
    }

    @Test
    fun shouldParseSinceXUntilYCommand() {
        // GIVEN
        val expectedStart = LocalDate.now().atStartOfDay()
                .withHour(12)
        val expectedEnd = LocalDate.now().atStartOfDay()
                .withHour(13)
        val expectedItem = TimeTrackingItem("comment",
                expectedStart, expectedEnd)
        // WHEN
        val result = executeCommand("comment since 12:00 until 13:00")

        // THEN
        assertThat(result, `is`(Optional.of(expectedItem)))
    }

    @Test
    fun shouldParseFromToWithSpaceInComment() {
        // GIVEN

        // WHEN
        val result = executeCommand("no t from 2014.06.22 14:43:14 to 2014.06.22 14:58:41")

        // THEN
        val (_, start, end) = result.get()
        assertThat(start, `is`(LocalDateTime.of(2014, 6, 22, 14, 43, 14)))
        assertThat(end, `is`(LocalDateTime.of(2014, 6, 22, 14, 58, 41)))
    }

    @Test
    fun shouldParseFromXtoYWithoutFromCommand() {

        // GIVEN
        val expectedStart = LocalDate.now().atStartOfDay()
                .withHour(12)
        val expectedEnd = LocalDate.now().atStartOfDay()
                .withHour(13)
        val expectedItem = TimeTrackingItem("com ment",
                expectedStart, expectedEnd)
        // WHEN
        val result = executeCommand("com ment 12:00 to 13:00")

        // THEN
        assertThat(result, `is`(Optional.of(expectedItem)))
    }

    @Test
    fun shouldDoNothingOnResumeLastAndActiveItem() {
        // GIVEN
        val unfinishedItem = createUnfinishedItem()
        givenCurrentTimeTrackingItem(unfinishedItem)

        // WHEN
        executeCommand("resume last")

        // THEN
        val timeTrackingItem = timeTrackingItemQueries.ongoingItem
        assertThat(timeTrackingItem, `is`(unfinishedItem))
    }

    @Test
    fun shouldStartNewItemNowOnResumeLastAndPreviouslyFinishedItem() {
        // GIVEN
        val finishedItem = TimeTrackingItem("last item",
                LocalDateTime.of(2014, 6, 22, 14, 43, 14),
                LocalDateTime.of(2015, 6, 22, 14, 43, 14))
        givenCurrentTimeTrackingItem(finishedItem)

        // WHEN
        executeCommand("resume last")

        // THEN
        val timeTrackingItem = timeTrackingItemQueries.ongoingItem!!
        assertThat(timeTrackingItem.activity, `is`("last item"))
        assertThat(!timeTrackingItem.start.isAfter(LocalDateTime.now()), `is`(true))
        assertThat(timeTrackingItem.end, `is`(nullValue()))
    }


    @Test
    @Throws(IOException::class)
    fun shouldEndCurrentItemOnFINCommand() {
        // GIVEN
        val unfinished = createUnfinishedItem()
        givenCurrentTimeTrackingItem(unfinished)

        // WHEN
        executeCommand("fin")

        // THEN
        val (_, _, end) = retrieveWrittenTimeTrackingItem()
        assertThat<LocalDateTime>(end, not(Optional.empty<Any>()))
    }

    private fun createUnfinishedItem(): TimeTrackingItem {
        return TimeTrackingItem("", LocalDateTime.now().minus(1, ChronoUnit.MILLIS))
    }

    @Test
    @Throws(IOException::class)
    fun shouldWriteCommandsAsNewItems() {
        // GIVEN

        // WHEN
        executeCommand("test")

        assertThatNewItemWasWritten("test")
    }

    @Throws(IOException::class)
    private fun assertThatNewItemWasWritten(testComment: String) {
        val (activity) = retrieveWrittenTimeTrackingItem()
        assertThat(activity, `is`(testComment))
    }

    @Theory
    fun shouldParseMinutesAgoFormats(
            @TestedOn(ints = intArrayOf(0, 1, 10, 61)) minutesAgo: Int, format: Command) {
        Assume.assumeTrue(format.isCategory("mins"))
        // GIVEN
        val command = format.supplyCommandFor(minutesAgo)

        // WHEN
        val (_, start) = retrieveItemWhenCommandIsExecuted(command)

        // THEN
        assertThat("Parameters: '$minutesAgo' '$command'",
                start,
                `is`<ChronoLocalDateTime<*>>(Matchers.lessThanOrEqualTo<ChronoLocalDateTime<*>>(LocalDateTime.now()
                        .minusMinutes(minutesAgo.toLong()))))
    }

    @Theory
    fun shouldParseSecondsAgoFormats(
            @TestedOn(ints = intArrayOf(0, 1, 10, 61)) secondsAgo: Int, format: Command) {
        Assume.assumeTrue(format.isCategory("secs"))
        // GIVEN
        val command = format.supplyCommandFor(secondsAgo)

        // WHEN
        val (_, start) = retrieveItemWhenCommandIsExecuted(command)

        // THEN
        assertThat("Parameters: '$secondsAgo' '$command'",
                start,
                `is`<ChronoLocalDateTime<*>>(Matchers.lessThanOrEqualTo<ChronoLocalDateTime<*>>(LocalDateTime.now()
                        .minusSeconds(secondsAgo.toLong()))))
    }

    @Theory
    fun shouldParseHourAgoFormats(
            @TestedOn(ints = intArrayOf(0, 1, 10, 61)) hoursAgo: Int, format: Command) {
        Assume.assumeTrue(format.isCategory("hours"))
        // GIVEN
        val command = format.supplyCommandFor(hoursAgo)

        // WHEN
        val (_, start) = retrieveItemWhenCommandIsExecuted(command)

        // THEN
        assertThat("Parameters: '$hoursAgo' '$command'",
                start,
                `is`<ChronoLocalDateTime<*>>(Matchers.lessThanOrEqualTo<ChronoLocalDateTime<*>>(LocalDateTime.now()
                        .minusHours(hoursAgo.toLong()))))
    }

    @Test
    fun shouldAddNonOverlappingPrecedingItem() {
        // GIVEN
        executeCommand("aaa from 2014.06.22 10:00 to 2014.06.22 16:00")

        // WHEN
        executeCommand("bbb from 2014.06.21 10:00 to 2014.06.21 16:00")

        // THEN
        val timeTrackingItems = timeTrackingItemQueries.queryAllItems()
                .toArray { arrayOfNulls<TimeTrackingItem>(it) }
        assertThat(timeTrackingItems[1]!!.start.toLocalDate(), `is`(LocalDate.of(2014, 6, 22)))
    }

    private fun executeCommand(command: String): Optional<TimeTrackingItem> {
        val cmdToExec = sut.parse(command)
        val testCommandHandler = TestCommandHandler()
        cmdToExec.accept(testCommandHandler)
        cmdToExec.accept(activities)
        timeTrackingItemQueries.sourceChanged(null)
        return Optional.ofNullable(testCommandHandler.resultItem)
    }

    private class TestCommandHandler : CommandHandler {
        internal var resultItem: TimeTrackingItem? = null

        override fun addNewActivity(command: NewActivity) {
            resultItem = command.newItem
        }

        override fun endCurrentActivity(command: EndCurrentItem) {}

        override fun removeActivity(command: RemoveActivity) {

        }

        override fun removeActivityAndCloseGap(command: RemoveActivity) {

        }

        override fun resumeActivity(command: ResumeActivity) {

        }

        override fun resumeLastActivity(command: ResumeLastActivity) {}

        override fun bulkChangeActivity(itemsToChange: Collection<TimeTrackingItem>, activity: String) {}
    }

    private fun retrieveItemWhenCommandIsExecuted(command: String): TimeTrackingItem {
        executeCommand(command)
        return retrieveWrittenTimeTrackingItem()
    }

    private fun retrieveWrittenTimeTrackingItem(): TimeTrackingItem {
        val allItems = timeTrackingItemQueries.queryAllItems().toList()
        assertThat<Collection<TimeTrackingItem>>(allItems, IsCollectionWithSize.hasSize(1))
        return allItems.iterator().next()
    }

    private fun givenCurrentTimeTrackingItem(item: TimeTrackingItem) {
        itemWriter.persist(item)
    }

    class Command(private val commandString: String, private val category: String) {
        fun isCategory(category: String): Boolean {
            return this.category == category
        }

        fun supplyCommandFor(amount: Int): String {
            return commandString.replace("%s", Integer.toString(amount))
        }
    }

    companion object {
        val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("y.M.d H:m[:s]")
        val TIME_FORMATTER = DateTimeFormatter.ofPattern("H:m[:s]")

        @JvmField
        @field:DataPoints
        var minuteFormats = arrayOf(min("test %smins ago"), min("test %s mins ago"), min("test %smin ago"), min("test\n%s minutes ago"), min("test one\ntest two %smin ago "))

        @JvmField
        @field:DataPoints
        var secondFormats = arrayOf(secs("test %ss ago"), secs("test %s sec ago"), secs("test %ssecs ago"), secs("test\n%s second ago"), secs("test %sseconds ago"), secs("from here to there %sseconds ago"))

        @JvmField
        @field:DataPoints
        var hourFormats = arrayOf(hours("test %sh ago"), hours("test %shr ago"), hours("test %s hrs ago"), hours("test\n%shour ago"), hours("test %s hours ago"), hours("left 3 hours ago %s hours ago"))

        fun min(command: String): Command {
            return Command(command, "mins")
        }

        fun secs(command: String): Command {
            return Command(command, "secs")
        }

        fun hours(command: String): Command {
            return Command(command, "hours")
        }
    }

}