package se.uulm.snowballr.backend.fetcher.orchestrator

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.fetcher.FetcherOrchestrator
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.exception.FetcherException
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.repository.UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE
import java.sql.SQLException

@OptIn(ExperimentalCoroutinesApi::class)
class FetcherOrchestratorProcessJobTest : FetcherOrchestratorTest() {
    private val exampleFetchers = mapOf(
        "foo" to mapOf(
            "fooOp1" to "true",
            "fooOp2" to "10",
        ),
        "bar" to mapOf(
            "barOp1" to "xyz",
        ),
    )

    /**
     * Calls [FetcherOrchestrator.enqueue] with mocked values.
     */
    private suspend fun FetcherOrchestrator.enqueueTestJob(job: FetcherEnqueueJob, project: Project) {
        coEvery { projectRepoMock.getProjectById(job.projectPaper.projectId) } returns Result.success(project)
        coJustRun { paperRepoMock.ensurePaperExists(job.projectPaper.paperId) }

        this.enqueue(job)
    }

    @Test
    fun `When retrieving the origin paper fails, then no fetching is attempted`() =
        runOrchestratorTest { orchestrator ->
            val job = DataBuilder.createExampleFetcherEnqueueJob()
            val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)

            coEvery {
                paperRepoMock.getPaperById(job.projectPaper.paperId)
            } returns Result.failure(TestSpecificException())

            orchestrator.enqueueTestJob(job, project)

            assertFetchingFailure()
        }

    @Test
    fun `When a job fails, then the next job is still processed`() = runOrchestratorTest { orchestrator ->
        val failingJob = DataBuilder.createExampleFetcherEnqueueJob()
        val succeedingJob = DataBuilder.createExampleFetcherEnqueueJob()
        val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)

        coEvery {
            paperRepoMock.getPaperById(failingJob.projectPaper.paperId)
        } returns Result.failure(TestSpecificException())

        // succeeding job proceeds at least to fetching
        val paper = DataBuilder.createExamplePaper(id = succeedingJob.projectPaper.paperId)
        coEvery { paperRepoMock.getPaperById(succeedingJob.projectPaper.paperId) } returns Result.success(paper)
        coEvery { fetcherManagerMock.fetchBackwardReferences(any(), any(), any()) } returns emptySet()
        coEvery { fetcherManagerMock.fetchForwardReferences(any(), any(), any()) } returns emptySet()

        orchestrator.enqueueTestJob(failingJob, project)
        orchestrator.enqueueTestJob(succeedingJob, project)

        coVerify(atLeast = 1) { paperRepoMock.getPaperById(succeedingJob.projectPaper.paperId) }
    }

    @Nested
    inner class RunFetching {
        @ParameterizedTest
        @EnumSource(SnowballingType::class)
        fun `When fetching fails for all fetchers, then no papers are created or added`(type: SnowballingType) =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val fetcherName = "test"
                val project = DataBuilder.createExampleProject(
                    snowballingType = type,
                    fetchers = mapOf(fetcherName to emptyMap()),
                )
                val paper = DataBuilder.createExamplePaper()
                val fetcherPaper = paper.toFetcherPaper()

                coEvery { paperRepoMock.getPaperById(job.projectPaper.paperId) } returns Result.success(paper)
                if (type.isBackwardOrBoth) {
                    coEvery {
                        fetcherManagerMock.fetchBackwardReferences(fetcherName, fetcherPaper, emptyMap())
                    } throws FetcherException("backward fetching failed")
                }
                if (type.isForwardOrBoth) {
                    coEvery {
                        fetcherManagerMock.fetchForwardReferences(fetcherName, fetcherPaper, emptyMap())
                    } throws FetcherException("forward fetching failed")
                }

                orchestrator.enqueueTestJob(job, project)

                if (type == SnowballingType.FORWARD) {
                    coVerify(exactly = 0) { fetcherManagerMock.fetchBackwardReferences(any(), any(), any()) }
                }
                if (type == SnowballingType.BACKWARD) {
                    coVerify(exactly = 0) { fetcherManagerMock.fetchForwardReferences(any(), any(), any()) }
                }
                assertPaperCreationFailure()
            }

        @ParameterizedTest
        @EnumSource(SnowballingType::class)
        fun `When fetching fails for one fetcher, then other fetchers still produce results`(type: SnowballingType) =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val fetcher1Name = "test1"
                val fetcher2Name = "test2"
                val project = DataBuilder.createExampleProject(
                    snowballingType = type,
                    fetchers = mapOf(
                        fetcher1Name to emptyMap(),
                        fetcher2Name to emptyMap(),
                    ),
                )
                val paper = DataBuilder.createExamplePaper()
                val fetcherPaper = paper.toFetcherPaper()
                val fetchedPaper = DataBuilder.createExampleFetcherPaper()

                coEvery { paperRepoMock.getPaperById(job.projectPaper.paperId) } returns Result.success(paper)
                if (type.isBackwardOrBoth) {
                    coEvery {
                        fetcherManagerMock.fetchBackwardReferences(fetcher1Name, fetcherPaper, emptyMap())
                    } throws FetcherException("backward fetching failed")
                    coEvery {
                        fetcherManagerMock.fetchBackwardReferences(fetcher2Name, fetcherPaper, emptyMap())
                    } returns setOf(fetchedPaper)
                }
                if (type.isForwardOrBoth) {
                    coEvery {
                        fetcherManagerMock.fetchForwardReferences(fetcher1Name, fetcherPaper, emptyMap())
                    } throws FetcherException("forward fetching failed")
                    coEvery {
                        fetcherManagerMock.fetchForwardReferences(fetcher2Name, fetcherPaper, emptyMap())
                    } returns setOf(fetchedPaper)
                }

                // Stop at paper creation
                coEvery {
                    paperRepoMock.getPaperByExternalIds(fetchedPaper.externalId.orEmpty())
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                if (type == SnowballingType.FORWARD) {
                    coVerify(exactly = 0) { fetcherManagerMock.fetchBackwardReferences(any(), any(), any()) }
                }
                if (type == SnowballingType.BACKWARD) {
                    coVerify(exactly = 0) { fetcherManagerMock.fetchForwardReferences(any(), any(), any()) }
                }
                assertPaperCreationFailure()
            }
    }

    @Nested
    inner class RunPaperCreation {
        @Test
        fun `When fetched papers already exist, then they are not created again`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(externalId = "BackwardId")
                val forwardRef = DataBuilder.createExamplePaper(externalId = "ForwardId")

                mockRunFetching(job, setOf(backwardRef.toFetcherPaper()), setOf(forwardRef.toFetcherPaper()))
                coEvery {
                    paperRepoMock.getPaperByExternalIds(backwardRef.externalId.orEmpty())
                } returns Result.success(backwardRef)
                coEvery {
                    paperRepoMock.getPaperByExternalIds(forwardRef.externalId.orEmpty())
                } returns Result.success(forwardRef)

                // Stop at paper citation
                coEvery {
                    citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id)
                } throws SQLException("Citing backwards failed")
                coEvery {
                    citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id)
                } throws SQLException("Citing forwards failed")
                coEvery {
                    projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id)
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                coVerify(exactly = 0) { paperRepoMock.createPaper(any()) }
                assertAddingPapersToProjectFailure()
            }

        @ParameterizedTest
        @CsvSource("externalId", "NULL", nullValues = ["NULL"])
        fun `When fetched papers don't already exist, then they are created`(externalId: String?) =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = externalId)
                val backwardFetcherRef = backwardRef.toFetcherPaper()
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = externalId)
                val forwardFetcherRef = forwardRef.toFetcherPaper()

                mockRunFetching(job, setOf(backwardFetcherRef), setOf(forwardFetcherRef))
                if (externalId != null) {
                    coEvery {
                        paperRepoMock.getPaperByExternalIds(backwardRef.externalId.orEmpty())
                    } returns Result.failure(TestSpecificException())
                    coEvery {
                        paperRepoMock.getPaperByExternalIds(forwardRef.externalId.orEmpty())
                    } returns Result.failure(TestSpecificException())
                }
                coEvery {
                    paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(backwardFetcherRef))
                } returns backwardRef
                coEvery {
                    paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(forwardFetcherRef))
                } returns forwardRef

                // Stop at adding papers to project
                coEvery {
                    citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id)
                } throws SQLException("Citing backwards failed")
                coEvery {
                    citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id)
                } throws SQLException("Citing forwards failed")
                coEvery {
                    projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id)
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                assertAddingPapersToProjectFailure()
                coVerify(exactly = 1) {
                    paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(backwardFetcherRef))
                }
                coVerify(exactly = 1) {
                    paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(forwardFetcherRef))
                }
            }

        @Test
        fun `When creating the DB papers fails, then no citations are created`() = runOrchestratorTest { orchestrator ->
            val job = DataBuilder.createExampleFetcherEnqueueJob()
            val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
            val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
            val backwardFetcherRef = backwardRef.toFetcherPaper()
            val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)
            val forwardFetcherRef = forwardRef.toFetcherPaper()

            mockRunFetching(job, setOf(backwardFetcherRef), setOf(forwardFetcherRef))
            coEvery {
                paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(backwardFetcherRef))
            } throws SQLException("Creating backward paper failed")
            coEvery {
                paperRepoMock.createPaper(CreatePaperRequest.fromFetcherPaper(forwardFetcherRef))
            } throws SQLException("Creating forward paper failed")

            orchestrator.enqueueTestJob(job, project)

            assertPaperCitationFailure()
        }
    }

    @Nested
    inner class RunPaperCitation {
        @Test
        fun `When citation creation fails with a non-duplicate error, then processing continues`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)

                mockRunPaperCreation(job, setOf(backwardRef), setOf(forwardRef))
                coEvery {
                    citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id)
                } throws SQLException("Citing backwards failed")
                coEvery {
                    citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id)
                } throws SQLException("Citing forwards failed")

                // Stop at adding papers to project
                coEvery {
                    projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id)
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                assertAddingPapersToProjectFailure()
                coVerify(exactly = 1) {
                    citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id)
                }
                coVerify(exactly = 1) {
                    citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id)
                }
            }

        @Test
        fun `When citation creation fails because of duplication, then processing continues`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)

                mockRunPaperCreation(job, setOf(backwardRef), setOf(forwardRef))
                coEvery {
                    citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id)
                } throws SQLException("Citing backwards failed", UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE)
                coEvery {
                    citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id)
                } throws SQLException("Citing forwards failed", UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE)

                // Stop at adding papers to project
                coEvery {
                    projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id)
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                assertAddingPapersToProjectFailure()
                coVerify(exactly = 1) {
                    citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id)
                }
                coVerify(exactly = 1) {
                    citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id)
                }
            }

        @Test
        fun `When creating the citation succeeds, then the processing proceeds`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)

                mockRunPaperCreation(job, setOf(backwardRef), setOf(forwardRef))
                coJustRun { citationRepoMock.addBackwardReferencedPaper(job.projectPaper.paperId, backwardRef.id) }
                coJustRun { citationRepoMock.addForwardReferencedPaper(job.projectPaper.paperId, forwardRef.id) }

                // Stop at adding papers to project
                coEvery {
                    projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id)
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                assertAddingPapersToProjectFailure()
            }
    }

    @Nested
    inner class RunAddingPapersToProject {
        @Test
        fun `When all papers already exist in the project, then no new papers are added to it`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)

                mockRunPaperCitation(job, setOf(backwardRef), setOf(forwardRef))
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id) } returns true
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, forwardRef.id) } returns true

                orchestrator.enqueueTestJob(job, project)

                assertAddingPapersToProjectFailure()
                coVerify(exactly = 0) { projectRepoMock.updateMaxStageIfExceeded(any(), any()) }
            }

        @Test
        fun `When bumping the max stage fails, then no papers are added to the project`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)

                mockRunPaperCitation(job, setOf(backwardRef), setOf(forwardRef))
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id) } returns false
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, forwardRef.id) } returns false
                coEvery {
                    projectRepoMock.updateMaxStageIfExceeded(project.id, job.projectPaper.stage + 1)
                } throws TestSpecificException()

                orchestrator.enqueueTestJob(job, project)

                assertAddingPapersToProjectFailure()
            }

        @Test
        fun `When adding papers to the project fails, then then no papers are added to the project`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)
                val targetStage = job.projectPaper.stage + 1

                mockRunPaperCitation(job, setOf(backwardRef), setOf(forwardRef))
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id) } returns false
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, forwardRef.id) } returns false
                coJustRun { projectRepoMock.updateMaxStageIfExceeded(project.id, targetStage) }
                coEvery {
                    projectPaperRepoMock.addPaperToProject(
                        project.id,
                        backwardRef.id,
                        targetStage,
                        job.triggeringUserId,
                    )
                } throws SQLException("Adding backward paper to project failed")
                coEvery {
                    projectPaperRepoMock.addPaperToProject(project.id, forwardRef.id, targetStage, job.triggeringUserId)
                } throws SQLException("Adding forward paper to project failed")

                orchestrator.enqueueTestJob(job, project)

                // Only the two failure calls were made
                coVerify(exactly = 2) { projectPaperRepoMock.addPaperToProject(any(), any(), any(), any()) }
            }

        @Test
        fun `When papers are not already in the project, then they are added to it`() =
            runOrchestratorTest { orchestrator ->
                val job = DataBuilder.createExampleFetcherEnqueueJob()
                val project = DataBuilder.createExampleProject(fetchers = exampleFetchers)
                val backwardRef = DataBuilder.createExamplePaper(title = "Back", externalId = null)
                val forwardRef = DataBuilder.createExamplePaper(title = "For", externalId = null)
                val targetStage = job.projectPaper.stage + 1

                mockRunPaperCitation(job, setOf(backwardRef), setOf(forwardRef))
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, backwardRef.id) } returns false
                coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, forwardRef.id) } returns false
                coJustRun { projectRepoMock.updateMaxStageIfExceeded(project.id, targetStage) }
                coJustRun {
                    projectPaperRepoMock.addPaperToProject(
                        project.id,
                        backwardRef.id,
                        targetStage,
                        job.triggeringUserId,
                    )
                }
                coJustRun {
                    projectPaperRepoMock.addPaperToProject(project.id, forwardRef.id, targetStage, job.triggeringUserId)
                }

                orchestrator.enqueueTestJob(job, project)

                // Two successful calls were made
                coVerify(exactly = 2) { projectPaperRepoMock.addPaperToProject(any(), any(), any(), any()) }
            }
    }
}
