package org.stt

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class StatesTest {
    @Test
    fun shouldNotThrowWhenConditionIsTrue() {
        // GIVEN

        // WHEN
        States.requireThat(true, "should not fail")

        // THEN no exception thrown
    }

    @Test
    fun shouldThrowIllegalStateExceptionWhenConditionIsFalse() {
        // GIVEN
        val message = "condition failed"

        // WHEN / THEN
        assertThatThrownBy { States.requireThat(false, message) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage(message)
    }
}