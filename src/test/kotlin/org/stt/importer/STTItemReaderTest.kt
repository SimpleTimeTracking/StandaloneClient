package org.stt.importer

import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.stt.persistence.stt.STTItemReader
import java.io.StringReader
import java.time.LocalDateTime

class STTItemReaderTest {

    @Test
    fun multiLineCommentGetsImportedCorrectly() {

        // GIVEN
        val stringReader = StringReader(
                "2012-10-10_22:00:00 2012-11-10_22:00:01 this is\\n a multiline\\n string\\n")
        val theReader = STTItemReader(stringReader)

        // WHEN
        val readItem = theReader.read()

        // THEN
        Assert.assertEquals(
                "this is\n a multiline\n string\n",
                readItem!!.activity)
    }

    @Test
    fun onlyStartTimeGiven() {

        // GIVEN
        val stringReader = StringReader("2012-10-10_22:00:00")
        val theReader = STTItemReader(stringReader)

        // WHEN
        val readItem = theReader.read()

        // THEN
        val time = LocalDateTime.of(2012, 10, 10, 22, 0, 0)
        Assert.assertThat(time, Matchers.equalTo(readItem!!.start))
    }

    @Test
    fun startTimeAndCommentGiven() {

        // GIVEN
        val stringReader = StringReader(
                "2012-10-10_22:00:00 the long comment")
        val theReader = STTItemReader(stringReader)

        // WHEN
        val readItem = theReader.read()

        // THEN
        val time = LocalDateTime.of(2012, 10, 10, 22, 0, 0)
        Assert.assertThat(time, Matchers.equalTo(readItem!!.start))
        Assert.assertThat("the long comment",
                Matchers.equalTo(readItem.activity))
    }
}
