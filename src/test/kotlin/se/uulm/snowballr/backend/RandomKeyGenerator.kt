package se.uulm.snowballr.backend

import io.jsonwebtoken.io.Encoders
import se.uulm.snowballr.backend.auth.JwtService
import java.security.KeyPairGenerator

/**
 * Helper object for generating keys that can be used for tests.
 */
object RandomKeyGenerator {
    /**
     * Generates a key pair with the specified [keySize] and returns their base64 representation.
     *
     * @param keySize The size of the keys in bytes.
     * @return A [Pair] with the private key in the first element and the public key in the second one.
     */
    fun generateKeyPair(keySize: Int = 2048): Pair<String, String> {
        val keyGen = KeyPairGenerator.getInstance(JwtService.KEY_ALGORITHM)
        keyGen.initialize(keySize)
        val keyPair = keyGen.generateKeyPair()

        val privateKey = keyPair.private
        val privateKeyBase64 = Encoders.BASE64.encode(privateKey.encoded)
        val publicKey = keyPair.public
        val publicKeyBase64 = Encoders.BASE64.encode(publicKey.encoded)

        return privateKeyBase64 to publicKeyBase64
    }
}
