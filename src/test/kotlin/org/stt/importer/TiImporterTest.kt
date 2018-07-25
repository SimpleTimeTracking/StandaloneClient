package org.stt.importer

import org.hamcrest.Matchers.*
import org.junit.Assert
import org.junit.Test
import org.stt.ti.importer.TiImporter
import java.io.StringReader

class TiImporterTest {

    @Test(expected = IllegalStateException::class)
    fun readingInvalidLineThrowsException() {

        // GIVEN
        val inputString = "2010-10-20"

        // WHEN
        val tiImporter = TiImporter(StringReader(inputString))
        tiImporter.read()

        // THEN
        // IllegalStateException
    }

    @Test
    fun readingValidFileReturnsOneItemPerLine() {
        // GIVEN
        val inputString = ("line1 2010-10-10_20:20:20 to 2010-10-10_20:20:30\n\r\n"
                + "line2 2010-10-10_20:20:20 to 2010-10-10_20:20:30\n\n"
                + "line3 2010-10-10_20:20:20 to 2010-10-10_20:20:30\n\n\n\n")

        // WHEN
        val importer = TiImporter(StringReader(inputString))
        val readItems = IOUtil.readAll(importer)

        // THEN
        Assert.assertEquals(3, readItems.size.toLong())
    }

    @Test
    fun commentIsParsedCorrectly() {
        // GIVEN
        val inputString = "the_long_comment 2014-10-12_13:24:35 to 2014-10-12_14:24:35\n" + "the_long_comment2 2014-10-13_13:24:35 to 2014-10-13_14:24:35\n"

        // WHEN
        val importer = TiImporter(StringReader(inputString))
        val readItems = IOUtil.readAll(importer)

        // THEN
        Assert.assertThat(
                readItems,
                contains(
                        hasProperty("activity",
                                `is`("the long comment")),
                        hasProperty("activity",
                                `is`("the long comment2"))))
    }
}
