package se.uulm.snowballr.backend.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordUtilsTest {
    @Test
    fun `When hashing and verifying the correct password, then verification succeeds`() {
        val plainPassword = "password1234"
        val hashedPassword = PasswordUtils.hashPassword(plainPassword)

        assertTrue(PasswordUtils.verifyPassword(plainPassword, hashedPassword))
    }

    @Test
    fun `When verifying an incorrect password against a hashed password, then verification fails`() {
        val plainPassword = "password1234"
        val wrongPassword = "password12345"
        val hashedPassword = PasswordUtils.hashPassword(plainPassword)

        assertFalse(PasswordUtils.verifyPassword(wrongPassword, hashedPassword))
    }
}
