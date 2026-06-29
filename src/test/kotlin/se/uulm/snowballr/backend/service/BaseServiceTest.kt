package se.uulm.snowballr.backend.service

import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import se.uulm.snowballr.backend.context.RequestContext

/**
 * Base test class for service-specific base test classes.
 *
 * This provides the setup and teardown logic for the service-specific dependencies.
 *
 * A fresh [RequestContext] is bound to the current thread before each test, so service code that reads
 * the current user, authentication status, or cookie sink works without a transport. Individual tests
 * populate it (e.g. via `RequestContext.current().userId = ...`).
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
interface BaseServiceTest {
    @BeforeEach
    fun setUpTest() {
        RequestContext.bind(RequestContext())
    }

    @AfterEach
    fun tearDownTest() {
        RequestContext.unbind()
        checkUnnecessaryStub(*getAllMocks())
        clearAllMocks()
    }

    fun getAllMocks(): Array<Any>
}
