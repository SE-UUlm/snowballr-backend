package se.uulm.snowballr.backend.fetcher.orchestrator

import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.fetcher.FetcherOrchestrator
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo

sealed class FetcherOrchestratorTest {
    val fetcherManagerMock = mockk<IFetcherManager>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val paperRepoMock = mockk<IPaperTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()

    private val allMocks = arrayOf(
        fetcherManagerMock,
        projectRepoMock,
        paperRepoMock,
        citationRepoMock,
        projectPaperRepoMock,
    )

    @AfterEach
    fun tearDownTest() {
        checkUnnecessaryStub(*allMocks)
        clearAllMocks()
    }

    /**
     * Creates a [FetcherOrchestrator] with the mocked dependencies and an [UnconfinedTestDispatcher] with the passed
     * [scheduler].
     *
     * We use an [UnconfinedTestDispatcher] so that after [FetcherOrchestrator.enqueue] returns,
     * [FetcherOrchestrator.processJob] has already processed the job, and we can verify the internal mocks.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun orchestrator(scheduler: TestCoroutineScheduler) = FetcherOrchestrator(
        fetcherManager = fetcherManagerMock,
        projectRepo = projectRepoMock,
        paperRepo = paperRepoMock,
        citationRepo = citationRepoMock,
        projectPaperRepo = projectPaperRepoMock,
        dispatcher = UnconfinedTestDispatcher(scheduler),
    )

    /**
     * Wrapper for [runTest] that creates and starts a [FetcherOrchestrator], passes it to [testBody] and stops the
     * orchestrator afterward.
     */
    protected fun runOrchestratorTest(testBody: suspend TestScope.(FetcherOrchestrator) -> Unit) = runTest {
        val orchestrator = orchestrator(testScheduler)
        orchestrator.start()

        testBody(orchestrator)

        orchestrator.stop()
    }

    protected fun assertFetchingFailure() {
        coVerify(exactly = 0) { fetcherManagerMock.fetchForwardReferences(any(), any(), any()) }
        coVerify(exactly = 0) { fetcherManagerMock.fetchBackwardReferences(any(), any(), any()) }

        assertPaperCreationFailure()
    }

    protected fun assertPaperCreationFailure() {
        coVerify(exactly = 0) { paperRepoMock.createPaper(any()) }

        assertPaperCitationFailure()
    }

    protected fun assertPaperCitationFailure() {
        coVerify(exactly = 0) { citationRepoMock.addBackwardReferencedPaper(any(), any()) }
        coVerify(exactly = 0) { citationRepoMock.addForwardReferencedPaper(any(), any()) }

        assertAddingPapersToProjectFailure()
    }

    protected fun assertAddingPapersToProjectFailure() {
        coVerify(exactly = 0) { projectPaperRepoMock.addPaperToProject(any(), any(), any(), any()) }
    }

    protected fun mockRunFetching(
        job: FetcherEnqueueJob,
        backwardRefs: Set<FetcherPaper>,
        forwardRefs: Set<FetcherPaper>,
    ) {
        val paper = DataBuilder.createExamplePaper(id = job.projectPaper.paperId)
        val fetcherPaper = paper.toFetcherPaper()

        coEvery { paperRepoMock.getPaperById(job.projectPaper.paperId) } returns Result.success(paper)
        coEvery {
            fetcherManagerMock.fetchBackwardReferences(any(), fetcherPaper, any())
        } returns backwardRefs
        coEvery {
            fetcherManagerMock.fetchForwardReferences(any(), fetcherPaper, any())
        } returns forwardRefs
    }

    protected fun mockRunPaperCreation(job: FetcherEnqueueJob, backwardRefs: Set<Paper>, forwardRefs: Set<Paper>) {
        val backwardFetcherRefs = backwardRefs.map(Paper::toFetcherPaper).toSet()
        val forwardFetcherRefs = forwardRefs.map(Paper::toFetcherPaper).toSet()

        mockRunFetching(job, backwardFetcherRefs, forwardFetcherRefs)
        for (backwardRef in backwardRefs) {
            val backwardFetcherRef = backwardRef.toFetcherPaper()
            coEvery {
                paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(backwardFetcherRef))
            } returns backwardRef
        }
        for (forwardRef in forwardRefs) {
            val forwardFetcherRef = forwardRef.toFetcherPaper()
            coEvery {
                paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(forwardFetcherRef))
            } returns forwardRef
        }
    }

    protected fun mockRunPaperCitation(job: FetcherEnqueueJob, backwardRefs: Set<Paper>, forwardRefs: Set<Paper>) {
        mockRunPaperCreation(job, backwardRefs, forwardRefs)
        for (backwardRef in backwardRefs) {
            coJustRun { citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id) }
        }
        for (forwardRef in forwardRefs) {
            coJustRun { citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id) }
        }
    }
}
