package org.stt.update

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Test

class VersionComparatorTest {
    private val sut = VersionComparator()

    @Test
    fun moreDigitsShouldMeanHigherVersion() {
        // GIVEN

        // WHEN
        val result = sut.compare("111", "99")

        // THEN
        assertThat(result, greaterThan(0))
    }

    @Test
    fun missingVersionPartMeansLowerVersion() {
        // GIVEN

        // WHEN
        val result = sut.compare("1.1.1", "1.1")

        // THEN
        assertThat(result, greaterThan(0))
    }

    @Test
    fun sameVersionMeanEqual() {
        // GIVEN

        // WHEN
        val result = sut.compare("1.1.1", "1.1.1")

        // THEN
        assertThat(result, `is`(0))
    }

    @Test
    fun higherVersionShouldBeHigher() {
        // GIVEN

        // WHEN
        val result = sut.compare("2-SNAPSHOT", "1.1.1")

        // THEN
        assertThat(result, `is`(1))
    }

}