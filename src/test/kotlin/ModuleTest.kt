package se.uulm.snowballr.backend

import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class ModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `Verify snowballr module`() {
        snowballRModule.verify()
    }
}
