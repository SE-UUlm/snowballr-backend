package se.uulm.snowballr.backend.fetcher.orchestrator

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import snowballr.ProjectOuterClass.SnowballingType

@OptIn(ExperimentalCoroutinesApi::class)
class FetcherOrchestratorEnqueueTest : FetcherOrchestratorTest() {
    @Test
    fun `When a job is enqueued without the orchestrator being started, then an IllegalStateException is thrown`() =
        runTest {
            val orchestrator = orchestrator(testScheduler)
            val job = DataBuilder.createExampleFetcherEnqueueJob()

            assertThrows<IllegalStateException> { orchestrator.enqueue(job) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() =
        runOrchestratorTest { orchestrator ->
            val job = DataBuilder.createExampleFetcherEnqueueJob()

            coEvery {
                projectRepoMock.getProjectById(job.projectPaper.projectId)
            } returns Result.failure(TestSpecificException())

            assertThrows<TestSpecificException> { orchestrator.enqueue(job) }
        }

    @Test
    fun `When the project's snowballing type is UNSPECIFIED, then the job is not enqueued`() =
        runOrchestratorTest { orchestrator ->
            val job = DataBuilder.createExampleFetcherEnqueueJob()
            val project =
                DataBuilder.createExampleProject(snowballingType = SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED)

            coEvery { projectRepoMock.getProjectById(job.projectPaper.projectId) } returns Result.success(project)

            orchestrator.enqueue(job)

            // Method returns before paper existence can be checked
            coVerify(exactly = 0) { paperRepoMock.ensurePaperExists(any()) }
        }

    @Test
    fun `When the project's list of fetchers is empty, then the job is not enqueued`() =
        runOrchestratorTest { orchestrator ->
            val job = DataBuilder.createExampleFetcherEnqueueJob()
            val project = DataBuilder.createExampleProject(fetchers = emptyMap())

            coEvery { projectRepoMock.getProjectById(job.projectPaper.projectId) } returns Result.success(project)

            orchestrator.enqueue(job)

            // Method returns before paper existence can be checked
            coVerify(exactly = 0) { paperRepoMock.ensurePaperExists(any()) }
        }

    @Test
    fun `When the origin paper doesn't exist, then a TestSpecificException is thrown`() =
        runOrchestratorTest { orchestrator ->
            val job = DataBuilder.createExampleFetcherEnqueueJob()
            val project = DataBuilder.createExampleProject(fetchers = mapOf(Pair("foo", emptyMap())))

            coEvery { projectRepoMock.getProjectById(job.projectPaper.projectId) } returns Result.success(project)
            coEvery { paperRepoMock.ensurePaperExists(job.projectPaper.paperId) } throws TestSpecificException()

            assertThrows<TestSpecificException> { orchestrator.enqueue(job) }
        }

    @Test
    fun `When the enqueue job data is valid, then the job is enqueued`() = runOrchestratorTest { orchestrator ->
        val job = DataBuilder.createExampleFetcherEnqueueJob()
        val project = DataBuilder.createExampleProject(fetchers = mapOf(Pair("foo", emptyMap())))

        coEvery { projectRepoMock.getProjectById(job.projectPaper.projectId) } returns Result.success(project)
        coJustRun { paperRepoMock.ensurePaperExists(job.projectPaper.paperId) }
        // We let this fail in the processJob method to verify that enqueuing was successful
        coEvery { paperRepoMock.getPaperById(job.projectPaper.paperId) } returns Result.failure(TestSpecificException())

        orchestrator.enqueue(job)

        coVerify(exactly = 1) { paperRepoMock.getPaperById(job.projectPaper.paperId) }
    }
}
