package org.stt.submit.csv

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests for [CsvUtil.escapeCell].
 *
 * Verifies plain values are passed through unchanged and values containing commas, quotes,
 * newlines, or carriage returns are wrapped in double quotes with embedded quotes doubled.
 */
class CsvUtilTest {

    @Test
    fun shouldReturnPlainValueUnchanged() {
        // GIVEN
        val value = "simple activity"

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEqualTo("simple activity")
    }

    @Test
    fun shouldQuoteValueContainingComma() {
        // GIVEN
        val value = "activity, with comma"

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEqualTo("\"activity, with comma\"")
    }

    @Test
    fun shouldQuoteValueContainingQuoteAndDoubleIt() {
        // GIVEN
        val value = "activity \"quoted\""

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEqualTo("\"activity \"\"quoted\"\"\"")
    }

    @Test
    fun shouldQuoteValueContainingNewline() {
        // GIVEN
        val value = "activity\nwith newline"

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEqualTo("\"activity\nwith newline\"")
    }

    @Test
    fun shouldQuoteValueContainingCarriageReturn() {
        // GIVEN
        val value = "activity\rwith cr"

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEqualTo("\"activity\rwith cr\"")
    }

    @Test
    fun shouldQuoteAndEscapeValueWithAllSpecialChars() {
        // GIVEN
        val value = "a,b\nc\r\"d\""

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEqualTo("\"a,b\nc\r\"\"d\"\"\"")
    }

    @Test
    fun shouldReturnEmptyStringUnchanged() {
        // GIVEN
        val value = ""

        // WHEN
        val result = CsvUtil.escapeCell(value)

        // THEN
        assertThat(result).isEmpty()
    }
}
