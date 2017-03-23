package org.stt.config;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class PasswordSettingTest {
    @Test
    public void shouldEncryptAndDecryptPassword() throws UnsupportedEncodingException {
        // GIVEN
        byte[] originalPassword = "Hello World".getBytes("UTF-8");

        // WHEN
        PasswordSetting passwordSetting = PasswordSetting.fromPassword(originalPassword);

        // THEN
        assertThat(passwordSetting.getPassword(), is(originalPassword));
    }

    @Test
    public void shouldNotStorePlainPassword() throws UnsupportedEncodingException {
        // GIVEN
        byte[] originalPassword = "Hello World".getBytes("UTF-8");

        // WHEN
        PasswordSetting passwordSetting = PasswordSetting.fromPassword(originalPassword);

        // THEN
        assertThat(passwordSetting.encodedPassword, is(not(originalPassword)));
    }

}