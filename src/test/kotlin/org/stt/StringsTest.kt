package org.stt

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringsTest {
    @Test
    fun shouldReturnFullStringWhenCommonPrefixIsWholeString() {
        // GIVEN
        val a = "hello"
        val b = "hello world"

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEqualTo("hello")
    }

    @Test
    fun shouldReturnEmptyStringWhenNoCommonPrefix() {
        // GIVEN
        val a = "abc"
        val b = "xyz"

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnPartialCommonPrefix() {
        // GIVEN
        val a = "abcdef"
        val b = "abcxyz"

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEqualTo("abc")
    }

    @Test
    fun shouldReturnShorterStringWhenOneIsPrefixOfOther() {
        // GIVEN
        val a = "test"
        val b = "testing"

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEqualTo("test")
    }

    @Test
    fun shouldReturnFullStringWhenStringsAreEqual() {
        // GIVEN
        val a = "same"
        val b = "same"

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEqualTo("same")
    }

    @Test
    fun shouldReturnFirstStringWhenSecondStringIsEmpty() {
        // GIVEN
        val a = "nonempty"
        val b = ""

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEqualTo("nonempty")
    }

    @Test
    fun shouldReturnEmptyStringWhenFirstStringIsEmpty() {
        // GIVEN
        val a = ""
        val b = "nonempty"

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun shouldReturnEmptyStringWhenBothStringsAreEmpty() {
        // GIVEN
        val a = ""
        val b = ""

        // WHEN
        val result = Strings.commonPrefix(a, b)

        // THEN
        assertThat(result).isEmpty()
    }
}