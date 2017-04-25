package org.stt.update;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class VersionComparatorTest {
    private VersionComparator sut = new VersionComparator();

    @Test
    public void moreDigitsShouldMeanHigherVersion() {
        // GIVEN

        // WHEN
        int result = sut.compare("111", "99");

        // THEN
        assertThat(result, greaterThan(0));
    }

    @Test
    public void missingVersionPartMeansLowerVersion() {
        // GIVEN

        // WHEN
        int result = sut.compare("1.1.1", "1.1");

        // THEN
        assertThat(result, greaterThan(0));
    }

    @Test
    public void sameVersionMeanEqual() {
        // GIVEN

        // WHEN
        int result = sut.compare("1.1.1", "1.1.1");

        // THEN
        assertThat(result, is(0));
    }

}