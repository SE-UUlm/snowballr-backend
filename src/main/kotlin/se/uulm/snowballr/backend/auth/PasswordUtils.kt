package se.uulm.snowballr.backend.auth

import com.password4j.Password

/**
 * Utility object for handling password hashing and verification, using the
 * [Password4j library](https://github.com/Password4j/password4j).
 */
object PasswordUtils {
    /**
     * Hashes a password using Argon2 with a random salt.
     *
     * @param password The plain text password to hash.
     * @return The hashed password as a string.
     */
    fun hashPassword(password: String): String = Password
        .hash(password)
        .addRandomSalt()
        .withArgon2()
        .result

    /**
     * Verifies a plain text password against a hashed password.
     *
     * @param password The plain text password to verify.
     * @param passwordHash The hashed password to compare against.
     * @return True if the password matches the hash, false otherwise.
     */
    fun verifyPassword(password: String, passwordHash: String): Boolean =
        Password.check(password, passwordHash).withArgon2()
}
