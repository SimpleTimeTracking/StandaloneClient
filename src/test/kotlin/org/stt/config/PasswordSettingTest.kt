package org.stt.config

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
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
        assertThat(passwordSetting.password, `is`(originalPassword))
    }

    @Test
    fun shouldNotStorePlainPassword() {
        // GIVEN
        val originalPassword = "Hello World".toByteArray(StandardCharsets.UTF_8)

        // WHEN
        val passwordSetting = PasswordSetting.fromPassword(originalPassword)

        // THEN
        assertThat(passwordSetting.encodedPassword, `is`(not(originalPassword)))
    }

}