package org.stt.reporting

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockitoAnnotations
import org.stt.config.PathSetting
import org.stt.config.WorktimeConfig
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

class WorkingtimeItemProviderTest {

    private var sut: WorkingtimeItemProvider? = null

    private val configuration = WorktimeConfig()

    @field:Rule
    @JvmField
    var tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        val tempFile = tempFolder.newFile()

        // populate test file
        FileUtils.write(tempFile,
                "2014-01-01 14\n2014-02-02 10 14", StandardCharsets.UTF_8)
        // end populate

        configuration.workingTimesFile = PathSetting(tempFile.absolutePath)

        sut = WorkingtimeItemProvider(configuration, "")
    }

    @Test
    fun defaultTimeOf8hIsReturnedIfNoDateGiven() {
        // GIVEN

        // WHEN
        val workingTimeFor = sut!!.getWorkingTimeFor(LocalDate.of(
                2014, 7, 1))

        // THEN
        assertThat(Duration.ofHours(8))
                .isEqualTo(workingTimeFor.min)
    }

    @Test
    fun configuredHoursForMondayIsUsed() {
        // GIVEN
        configuration.workingHours[DayOfWeek.MONDAY.name] = Duration.ofHours(10)

        // WHEN
        val workingTimeFor = sut!!.getWorkingTimeFor(LocalDate.of(
                2014, 7, 7))

        // THEN

        assertThat(Duration.ofHours(10))
                .isEqualTo(workingTimeFor.min)
    }

    @Test
    fun configuredTimeIsReturned() {
        // GIVEN

        // WHEN
        val workingTimeFor = sut!!.getWorkingTimeFor(LocalDate.of(
                2014, 1, 1))

        // THEN
        assertThat(Duration.ofHours(14))
                .isEqualTo(workingTimeFor.min)
    }

    @Test
    fun configuredMinMaxTimeIsReturned() {
        // GIVEN

        // WHEN
        val workingTimeFor = sut!!.getWorkingTimeFor(LocalDate.of(
                2014, 2, 2))

        // THEN
        val min = Duration.ofHours(10)
        val max = Duration.ofHours(14)
        assertThat(WorkingtimeItem(min, max)).isEqualTo(workingTimeFor)
    }
}
