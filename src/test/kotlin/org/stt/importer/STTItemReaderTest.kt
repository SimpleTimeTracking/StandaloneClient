package org.stt.importer

import org.assertj.core.api.Assertions.assertThat
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
        assertThat("this is\n a multiline\n string\n").isEqualTo(readItem!!.activity)
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
        assertThat(time).isEqualTo(readItem!!.start)
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
        assertThat(time).isEqualTo(readItem!!.start)
        assertThat("the long comment").isEqualTo(readItem.activity)
    }
}
