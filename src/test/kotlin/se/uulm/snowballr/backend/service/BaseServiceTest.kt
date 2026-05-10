package se.uulm.snowballr.backend.service

import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.mockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import se.uulm.snowballr.backend.auth.GrpcContext

/**
 * Base test class for service-specific base test classes.
 *
 * This provides the setup and teardown logic for the service-specific dependencies.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
interface BaseServiceTest {
    @BeforeEach
    fun setUpTest() {
        mockkObject(GrpcContext)
    }

    @AfterEach
    fun tearDownTest() {
        checkUnnecessaryStub(*getAllMocks())
        clearAllMocks()
    }

    fun getAllMocks(): Array<Any>
}
