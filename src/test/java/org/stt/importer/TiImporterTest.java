package org.stt.importer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemImporter;

import com.google.common.base.Optional;

public class TiImporterTest {
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test(expected = FileNotFoundException.class)
	public void readingOfNonExitentFileFails() throws IOException {
		// GIVEN
		File importFile = new File("this/file/does/not/exist");

		// WHEN
		ItemImporter importer = new TiImporter(new FileReader(importFile));
		importer.read();

		// THEN
		// FileNotFoundException expected
	}

	@Test
	public void readingValidFileReturnsOneItemPerLine() throws IOException {
		// GIVEN
		File tempFile = tempFolder.newFile();
		FileUtils.write(tempFile, "line1\nline2\nline3\n\n\n");

		// WHEN
		ItemImporter importer = new TiImporter(new FileReader(tempFile));
		Collection<TimeTrackingItem> readItems = IOUtil.readAll(importer);

		// THEN
		Assert.assertEquals(3, readItems.size());
	}

	@Test
	public void commentIsParsedCorrectly() throws IOException {
		// GIVEN
		File tempFile = tempFolder.newFile();
		FileUtils
				.write(tempFile,
						"the_long_comment 2014-10-12_13:24:35 to 2014-10-12_14:24:35\n"
								+ "the_long_comment2 2014-10-13_13:24:35 to 2014-10-13_14:24:35\n");

		// WHEN
		ItemImporter importer = new TiImporter(new FileReader(tempFile));
		Collection<TimeTrackingItem> readItems = IOUtil.readAll(importer);

		// THEN
		Assert.assertThat(
				readItems,
				contains(
						hasProperty("comment",
								is(Optional.of("the_long_comment"))),
						hasProperty("comment",
								is(Optional.of("the_long_comment2")))));
	}
}
