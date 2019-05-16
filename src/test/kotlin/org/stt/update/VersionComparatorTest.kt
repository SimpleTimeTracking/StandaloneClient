package org.stt.update

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VersionComparatorTest {
    private val sut = VersionComparator()

    @Test
    fun moreDigitsShouldMeanHigherVersion() {
        // GIVEN

        // WHEN
        val result = sut.compare("111", "99")

        // THEN
        assertThat(result).isGreaterThan(0)
    }

    @Test
    fun missingVersionPartMeansLowerVersion() {
        // GIVEN

        // WHEN
        val result = sut.compare("1.1.1", "1.1")

        // THEN
        assertThat(result).isGreaterThan(0)
    }

    @Test
    fun sameVersionMeanEqual() {
        // GIVEN

        // WHEN
        val result = sut.compare("1.1.1", "1.1.1")

        // THEN
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun higherVersionShouldBeHigher() {
        // GIVEN

        // WHEN
        val result = sut.compare("2-SNAPSHOT", "1.1.1")

        // THEN
        assertThat(result).isEqualTo(1)
    }

}