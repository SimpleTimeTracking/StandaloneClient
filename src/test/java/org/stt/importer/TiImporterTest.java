package org.stt.importer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.io.StringReader;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.stt.importer.ti.TiImporter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class TiImporterTest {

	@Test(expected = IllegalStateException.class)
	public void readingInvalidLineThrowsException() {

		// GIVEN
		String inputString = "2010-10-20";

		// WHEN
		TiImporter tiImporter = new TiImporter(new StringReader(inputString));
		tiImporter.read();

		// THEN
		// IllegalStateException
	}

	@Test
	public void readingValidFileReturnsOneItemPerLine() {
		// GIVEN
		String inputString = "line1 2010-10-10_20:20:20 to 2010-10-10_20:20:30\n\r\n"
				+ "line2 2010-10-10_20:20:20 to 2010-10-10_20:20:30\n\n"
				+ "line3 2010-10-10_20:20:20 to 2010-10-10_20:20:30\n\n\n\n";

		// WHEN
		ItemReader importer = new TiImporter(new StringReader(inputString));
		Collection<TimeTrackingItem> readItems = IOUtil.readAll(importer);

		// THEN
		Assert.assertEquals(3, readItems.size());
	}

	
	@SuppressWarnings("unchecked")
	@Test
	public void commentIsParsedCorrectly() {
		// GIVEN
		String inputString = "the_long_comment 2014-10-12_13:24:35 to 2014-10-12_14:24:35\n"
				+ "the_long_comment2 2014-10-13_13:24:35 to 2014-10-13_14:24:35\n";

		// WHEN
		ItemReader importer = new TiImporter(new StringReader(inputString));
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
