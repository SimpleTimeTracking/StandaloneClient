package org.stt.persistence;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.time.DateTimes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;

public class BackupCreatorTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Mock
	private Configuration configuration;

	private File currentTempFolder;
	private File currentSttFile;

	private BackupCreator sut;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);

		currentTempFolder = tempFolder.newFolder();
		currentSttFile = tempFolder.newFile();
        try (PrintWriter out = new PrintWriter(
                currentSttFile, "UTF8")) {
            out.print("blubb, just a test line");
        }

		given(configuration.getSttFile()).willReturn(currentSttFile);
		given(configuration.getBackupInterval()).willReturn(7);
		given(configuration.getBackupLocation()).willReturn(currentTempFolder);
		given(configuration.getBackupRetentionCount()).willReturn(3);

		sut = new BackupCreator(configuration);
	}

	@Test
	public void existingBackupShouldPreventNewBackup() throws IOException {
		// GIVEN
        String threeDaysAgo = DateTimes.prettyPrintDate(LocalDate.now()
                .minusDays(3));
        String sttFileName = currentSttFile.getName();
        File backedUp = new File(currentTempFolder, sttFileName + "-"
				+ threeDaysAgo);
		createNewFile(backedUp);

		// WHEN
		sut.start();

		// THEN
		Collection<File> files = FileUtils.listFiles(currentTempFolder,
				FileFileFilter.FILE, null);

		Assert.assertEquals(1, files.size());
		Assert.assertThat(files.iterator().next().getAbsoluteFile(),
				is(backedUp.getAbsoluteFile()));
	}

	@Test
	public void oldBackupShouldBeDeleted() throws IOException {
		// GIVEN
		for (int i = 0; i < configuration.getBackupRetentionCount(); i++) {
            String xDaysAgo = DateTimes.prettyPrintDate(LocalDate.now()
                    .minusDays(i));
            String sttFileName = currentSttFile.getName();
            File oldFile = new File(currentTempFolder, sttFileName + "-"
					+ xDaysAgo);
			createNewFile(oldFile);
		}

        String xDaysAgo = DateTimes.prettyPrintDate(LocalDate.now()
                .minusDays(configuration.getBackupRetentionCount() + 1));
        String sttFileName = currentSttFile.getName();
        File oldFile = new File(currentTempFolder, sttFileName + "-" + xDaysAgo);
		createNewFile(oldFile);

		// WHEN
		sut.start();

		// THEN
		Assert.assertFalse("Old backup file should have been deleted",
				oldFile.exists());
	}

	@Test
	public void initialBackupShouldBeCreated() throws IOException {
		// GIVEN
        String currentDate = DateTimes.prettyPrintDate(LocalDate.now());
        String sttFileName = currentSttFile.getName();
        File expectedFile = new File(currentTempFolder, sttFileName + "-"
                + currentDate);

		// WHEN
		sut.start();

		// THEN
		Assert.assertTrue(
				"Original and backed up files do not have the same contents",
				FileUtils.contentEquals(currentSttFile, expectedFile));
	}

	@Test
	public void existingFileShouldNotBeOverwritten() throws IOException {
		// GIVEN
        String currentDate = DateTimes.prettyPrintDate(LocalDate.now());
        String sttFileName = currentSttFile.getName();
        File existingFile = new File(currentTempFolder, sttFileName + "-"
                + currentDate);
		createNewFile(existingFile);

		// WHEN
		sut.start();

		// THEN
		Assert.assertFalse(
				"Original and backed up files do not have the same contents",
				FileUtils.contentEquals(currentSttFile, existingFile));
	}

	private void createNewFile(File toCreate) throws IOException {
		Assert.assertTrue(
				"could not create test file " + toCreate.getAbsolutePath(),
				toCreate.createNewFile());
	}
}
