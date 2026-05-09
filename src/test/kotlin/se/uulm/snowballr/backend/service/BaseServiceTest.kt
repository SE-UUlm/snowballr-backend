package se.uulm.snowballr.backend.service

import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.mockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.test.KoinTest
import se.uulm.snowballr.backend.auth.GrpcContext

/**
 * Base test class for service-specific base test classes.
 *
 * This provides the setup and teardown logic for the service-specific dependencies.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class BaseServiceTest : KoinTest {
    @BeforeEach
    fun setUpTest() {
        startKoin {
            modules(getModule())
        }
        mockkObject(GrpcContext)
    }

    @AfterEach
    open fun tearDownTest() {
        checkUnnecessaryStub(*getAllMocks())
        clearAllMocks()
        stopKoin()
    }

    abstract fun getModule(): Module

    abstract fun getAllMocks(): Array<Any>
}
