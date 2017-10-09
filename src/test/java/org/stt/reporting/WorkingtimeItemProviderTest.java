package org.stt.reporting;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;
import org.stt.config.PathSetting;
import org.stt.config.WorktimeConfig;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WorkingtimeItemProviderTest {

	private WorkingtimeItemProvider sut;

    private WorktimeConfig configuration = new WorktimeConfig();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);

		File tempFile = tempFolder.newFile();

		// populate test file
		FileUtils.write(tempFile,
				"2014-01-01 14\n2014-02-02 10 14", StandardCharsets.UTF_8);
		// end populate

        configuration.setWorkingTimesFile(new PathSetting(tempFile.getAbsolutePath()));

        sut = new WorkingtimeItemProvider(configuration, "");
    }

	@Test
	public void defaultTimeOf8hIsReturnedIfNoDateGiven() {
		// GIVEN

		// WHEN
        WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(LocalDate.of(
                2014, 7, 1));

		// THEN
        assertThat(Duration.ofHours(8),
                is(workingTimeFor.getMin()));
	}

	@Test
	public void configuredHoursForMondayIsUsed() {
		// GIVEN
        configuration.getWorkingHours().put(DayOfWeek.MONDAY.name(), Duration.ofHours(10));

		// WHEN
        WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(LocalDate.of(
                2014, 7, 7));

		// THEN

        assertThat(Duration.ofHours(10),
                is(workingTimeFor.getMin()));
	}

	@Test
	public void configuredTimeIsReturned() {
		// GIVEN

		// WHEN
        WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(LocalDate.of(
                2014, 1, 1));

		// THEN
        assertThat(Duration.ofHours(14),
                is(workingTimeFor.getMin()));
	}

	@Test
	public void configuredMinMaxTimeIsReturned() {
		// GIVEN

		// WHEN
        WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(LocalDate.of(
                2014, 2, 2));

		// THEN
        Duration min = Duration.ofHours(10);
        Duration max = Duration.ofHours(14);
        assertThat(new WorkingtimeItem(min, max), is(workingTimeFor));
	}
}
