package org.stt.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.StandardCharsets

class PasswordSettingTest {
    @Test
    fun shouldEncryptAndDecryptPassword() {
        // GIVEN
        val originalPassword = "Hello World".toByteArray(StandardCharsets.UTF_8)

        // WHEN
        val passwordSetting = PasswordSetting.fromPassword(originalPassword)

        // THEN
        assertThat(passwordSetting.password).isEqualTo(originalPassword)
    }

    @Test
    fun shouldNotStorePlainPassword() {
        // GIVEN
        val originalPassword = "Hello World".toByteArray(StandardCharsets.UTF_8)

        // WHEN
        val passwordSetting = PasswordSetting.fromPassword(originalPassword)

        // THEN
        assertThat(passwordSetting.encodedPassword).isNotEqualTo(originalPassword)
    }

}