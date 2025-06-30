package se.uulm.snowballr.backend

import io.jsonwebtoken.io.Encoders
import se.uulm.snowballr.backend.auth.JwtUtils
import java.security.KeyPairGenerator

object RandomKeyGenerator {
    /**
     * Generates a key pair with the specified [keySize] and returns their base64 representation.
     */
    fun generateKeyPair(keySize: Int = 2048): Pair<String, String> {
        val keyGen = KeyPairGenerator.getInstance(JwtUtils.KEY_ALGORITHM)
        keyGen.initialize(keySize)
        val keyPair = keyGen.generateKeyPair()

        val privateKey = keyPair.private
        val privateKeyBase64 = Encoders.BASE64.encode(privateKey.encoded)
        val publicKey = keyPair.public
        val publicKeyBase64 = Encoders.BASE64.encode(publicKey.encoded)

        return privateKeyBase64 to publicKeyBase64
    }
}
