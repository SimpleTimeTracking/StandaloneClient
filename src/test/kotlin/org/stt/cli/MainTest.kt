package org.stt.cli

import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockitoAnnotations
import org.stt.command.Activities
import org.stt.command.CommandFormatter
import org.stt.command.CommandTextParser
import org.stt.config.ConfigRoot
import org.stt.persistence.ItemReader
import org.stt.persistence.stt.STTItemPersister
import org.stt.persistence.stt.STTItemReader
import org.stt.query.TimeTrackingItemQueries
import org.stt.reporting.WorkingtimeItemProvider
import org.stt.text.WorktimeCategorizer
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Provider

class MainTest {
    private var sut: Main? = null

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    private var currentSttFile: File? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        val configRoot = ConfigRoot()

        currentSttFile = configRoot.sttFile.file(tempFolder.newFolder().absolutePath)
        val mkdirs = currentSttFile!!.parentFile.mkdirs()
        assertThat(mkdirs).isTrue()
        val newFile = currentSttFile!!.createNewFile()
        assertThat(newFile).isTrue()

        val sttReader = Provider<Reader> {
            try {
                InputStreamReader(FileInputStream(currentSttFile!!), StandardCharsets.UTF_8)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }

        val sttWriter = Provider<Writer> {
            try {
                OutputStreamWriter(FileOutputStream(currentSttFile!!), StandardCharsets.UTF_8)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
        val readerProvider = Provider<ItemReader> { STTItemReader(sttReader.get()) }
        val queries = TimeTrackingItemQueries(readerProvider, Optional.empty())
        val worktimeItemProvider = WorkingtimeItemProvider(configRoot.worktime, "")
        val categorizer = WorktimeCategorizer(configRoot.worktime)
        val reportPrinter = ReportPrinter(queries, configRoot.cli, worktimeItemProvider, categorizer)
        val persister = STTItemPersister(sttReader, sttWriter)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val commandFormatter = CommandFormatter(CommandTextParser(listOf(timeFormatter, dateTimeFormatter)), dateTimeFormatter, timeFormatter)
        val activities = Activities(persister, queries, Optional.empty())
        sut = Main(queries, reportPrinter, commandFormatter, activities)
    }

    @Test
    fun startingWorkWritesToConfiguredFile() {

        // GIVEN
        val expectedComment = "some long comment we are currently working on"
        val args = ArrayList<String>()
        val command = "on $expectedComment"
        args.addAll(Arrays.asList(*command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos, true, StandardCharsets.UTF_8.name())

        // WHEN
        sut!!.prepareAndExecuteCommand(args, ps)

        // THEN
        val readLines = IOUtils.readLines(InputStreamReader(
                FileInputStream(currentSttFile!!), StandardCharsets.UTF_8))
        assertThat(readLines)
                .anyMatch {
                    it.contains(expectedComment)
                }

        val returned = baos.toString(StandardCharsets.UTF_8.name())
        assertThat(returned).containsSequence(expectedComment)

        ps.close()
    }

}
