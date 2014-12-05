package org.stt.reporting;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.reporting.WorkingtimeItemProvider.WorkingtimeItem;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class WorkingtimeItemProviderTest {

	@Mock
	private Configuration configuration;
	private WorkingtimeItemProvider sut;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);

		File tempFile = tempFolder.newFile();

		// populate test file
		FileUtils.write(tempFile,
				"2014-01-01 14\nhoursMon = 10\n2014-02-02 10 14");
		// end populate

		given(configuration.getWorkingTimesFile()).willReturn(tempFile);

		sut = new WorkingtimeItemProvider(configuration);
	}

	@Test
	public void defaultTimeOf8hIsReturnedIfNoDateGiven() {
		// GIVEN

		// WHEN
		WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(new DateTime(
				2014, 7, 1, 0, 0, 0));

		// THEN
		assertThat(new Duration(8 * DateTimeConstants.MILLIS_PER_HOUR),
				is(workingTimeFor.getMin()));
	}

	@Test
	public void configuredHoursForMondayIsUsed() {
		// GIVEN

		// WHEN
		WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(new DateTime(
				2014, 7, 7, 0, 0, 0));

		// THEN

		assertThat(new Duration(10 * DateTimeConstants.MILLIS_PER_HOUR),
				is(workingTimeFor.getMin()));
	}

	@Test
	public void configuredTimeIsReturned() {
		// GIVEN

		// WHEN
		WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(new DateTime(
				2014, 1, 1, 0, 0, 0));

		// THEN
		assertThat(new Duration(14 * DateTimeConstants.MILLIS_PER_HOUR),
				is(workingTimeFor.getMin()));
	}

	@Test
	public void configuredMinMaxTimeIsReturned() {
		// GIVEN

		// WHEN
		WorkingtimeItem workingTimeFor = sut.getWorkingTimeFor(new DateTime(
				2014, 2, 2, 0, 0, 0));

		// THEN
		Duration min = new Duration(10 * DateTimeConstants.MILLIS_PER_HOUR);
		Duration max = new Duration(14 * DateTimeConstants.MILLIS_PER_HOUR);
		assertThat(new WorkingtimeItem(min, max), is(workingTimeFor));
	}
}
