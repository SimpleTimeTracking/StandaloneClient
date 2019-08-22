package org.stt.config

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class PasswordSetting private constructor(encodedPassword: ByteArray) {
    val encodedPassword: ByteArray
        get() = Arrays.copyOf(field, field.size)

    val password: ByteArray
        get() {
            try {
                val c = Cipher.getInstance(CIPHER_ALGORITHM)
                c.init(Cipher.DECRYPT_MODE, secretKey)
                return c.doFinal(encodedPassword)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }

        }

    init {
        this.encodedPassword = Arrays.copyOf(encodedPassword, encodedPassword.size)
        // Test the encrypted password
        password
    }

    companion object {
        private const val CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding"
        private var secretKey: SecretKey? = null

        init {
            val md: MessageDigest
            try {
                md = MessageDigest.getInstance("SHA-1")
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException(e)
            }

            val key: ByteArray
            key = Arrays.copyOf(md.digest("ImagineAReallyStrongPasswordHere".toByteArray(StandardCharsets.UTF_8)), 16)
            secretKey = SecretKeySpec(key, "AES")
        }

        fun fromPassword(password: ByteArray): PasswordSetting {
            try {
                val c = Cipher.getInstance(CIPHER_ALGORITHM)
                c.init(Cipher.ENCRYPT_MODE, secretKey)
                return PasswordSetting(c.doFinal(password))
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }

        }

        fun fromEncryptedPassword(password: ByteArray): PasswordSetting {
            return PasswordSetting(password)
        }
    }
}
