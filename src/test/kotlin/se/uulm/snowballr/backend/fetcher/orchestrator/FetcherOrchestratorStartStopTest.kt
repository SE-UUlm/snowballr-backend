package se.uulm.snowballr.backend.fetcher.orchestrator

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class FetcherOrchestratorStartStopTest : FetcherOrchestratorTest() {
    @Test
    fun `When the orchestrator is started and stopped, then no exception is thrown`() = runTest {
        val orchestrator = orchestrator(testScheduler)

        assertDoesNotThrow { orchestrator.start() }
        assertDoesNotThrow { orchestrator.stop() }
    }

    @Test
    fun `When the orchestrator is started twice, then an IllegalStateException is thrown`() = runTest {
        val orchestrator = orchestrator(testScheduler)

        orchestrator.start()
        assertThrows<IllegalStateException> { orchestrator.start() }
    }

    @Test
    fun `When the orchestrator is stopped without being started, then no exception is thrown`() = runTest {
        val orchestrator = orchestrator(testScheduler)

        assertDoesNotThrow { orchestrator.stop() }
    }

    @Test
    fun `When the orchestrator is started after being stopped, then an IllegalStateException is thrown`() = runTest {
        val orchestrator = orchestrator(testScheduler)

        orchestrator.stop()

        assertThrows<IllegalStateException> {
            orchestrator.start()
        }
    }
}
