package org.stt.config;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class PasswordSetting {
    public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static SecretKey secretKey;
    public final byte[] encodedPassword;

    static {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] key;
        key = Arrays.copyOf(md.digest("ImagineAReallyStrongPasswordHere".getBytes(StandardCharsets.UTF_8)), 16);
        secretKey = new SecretKeySpec(key, "AES");
    }

    private PasswordSetting(byte[] encodedPassword) {
        requireNonNull(encodedPassword);
        this.encodedPassword = Arrays.copyOf(encodedPassword, encodedPassword.length);
        // Test the encrypted password
        getPassword();
    }

    public byte[] getPassword() {
        try {
            Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, secretKey);
            return c.doFinal(encodedPassword);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static PasswordSetting fromPassword(byte[] password) {
        try {
            Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, secretKey);
            return new PasswordSetting(c.doFinal(password));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static PasswordSetting fromEncryptedPassword(byte[] password) {
        return new PasswordSetting(password);
    }
}
