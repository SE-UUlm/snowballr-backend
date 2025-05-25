package se.uulm.snowballr.backend

import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * Here we only test that the Koin module can be loaded and that all dependencies can be resolved.
 */
class ModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `Verify the SnowballR module`() {
        snowballRModule.verify()
    }
}
