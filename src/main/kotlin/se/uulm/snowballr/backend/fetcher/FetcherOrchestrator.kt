package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toFetcherPaper
import se.uulm.snowballr.backend.model.exception.FetcherException
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.model.fetcher.FetcherProcessingJob
import se.uulm.snowballr.backend.model.fetcher.FetchingDirection
import se.uulm.snowballr.backend.model.fetcher.FetchingResults
import se.uulm.snowballr.backend.model.fetcher.PaperCreationResults
import se.uulm.snowballr.backend.model.fetcher.toGrpcPaperRequest
import se.uulm.snowballr.backend.model.isBackwardOrBoth
import se.uulm.snowballr.backend.model.isForwardOrBoth
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.isUniqueConstraintViolation
import snowballr.ProjectOuterClass.SnowballingType
import java.sql.SQLException
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

private val logger = KotlinLogging.logger { }

interface IFetcherOrchestrator {
    /**
     * Start the fetcher orchestrator.
     *
     * An instance of [IFetcherOrchestrator] can only be started once and cannot be restarted again.
     *
     * @throws IllegalStateException if the fetcher orchestrator already is running or has been stopped before.
     */
    fun start()

    /**
     * Stop the fetcher orchestrator.
     */
    fun stop()

    /**
     * Enqueue a job to the fetcher orchestrator.
     *
     * @throws IllegalStateException if the fetcher orchestrator is not running.
     */
    suspend fun enqueue(job: FetcherEnqueueJob)
}

class FetcherOrchestrator(
    private val fetcherManager: IFetcherManager,
    private val projectRepo: IProjectTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IFetcherOrchestrator {
    private val queue = Channel<FetcherProcessingJob>(Channel.UNLIMITED)

    // Use supervisor job so that jobs can fail without cancelling the entire scope
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var isStarted = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun start() {
        check(!isStarted) { "Orchestrator is already running" }
        check(!queue.isClosedForSend) { "Orchestrator has been closed" }

        isStarted = true
        scope.launch {
            for (job in queue) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    processJob(job)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to process job: ${e.message}" }
                }
            }
        }

        logger.info { "Fetcher Orchestrator started" }
    }

    override fun stop() {
        scope.cancel()
        queue.close()
        isStarted = false

        logger.info { "Fetcher Orchestrator stopped" }
    }

    override suspend fun enqueue(job: FetcherEnqueueJob) {
        check(isStarted) { "Orchestrator has not been started yet" }

        val project = projectRepo.getProjectById(job.projectPaper.projectId).getOrThrow()

        if (project.snowballingType == SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED) {
            logger.warn { "Snowballing type is unspecified for project '${job.projectPaper.projectId}'." }
            return
        }

        if (project.fetchers.isEmpty()) {
            logger.warn { "No fetchers configured for project '${job.projectPaper.projectId}'." }
            return
        }

        paperRepo.ensurePaperExists(job.projectPaper.paperId)

        val processingJob = FetcherProcessingJob(
            projectId = project.id,
            fetchers = project.fetchers,
            snowballingType = project.snowballingType,
            targetStage = job.projectPaper.stage + 1,
            paperId = job.projectPaper.paperId,
            triggeringUserId = job.triggeringUserId,
        )

        val result = queue.trySend(processingJob)

        if (result.isSuccess) {
            logger.info {
                "Successfully enqueued job for paper ${job.projectPaper.id} in stage ${job.projectPaper.stage}"
            }
        } else {
            logger.error { "Failed to enqueue job for paper ${job.projectPaper.id} in stage ${job.projectPaper.stage}" }
        }
    }

    private suspend fun processJob(job: FetcherProcessingJob) {
        logger.info { "Starting fetcher processing job for paper ${job.paperId}" }

        val paper = paperRepo.getPaperById(job.paperId).getOrThrow()

        val fetchingResults = runFetching(job, paper)

        // TODO: paper filtering/merging
        // fetching from multiple fetchers will probably return the same papers from different fetchers
        // first merge fetched papers then merge with existing paper in DB (if existent)

        val creationResults = runPaperCreation(fetchingResults)

        runPaperCitation(job.paperId, creationResults)

        runAddingPapersToProject(job, creationResults)

        logger.info { "Finished fetcher processing job for paper ${job.paperId}" }
    }

    /**
     * Runs the fetching part of the job processing.
     *
     * Based on the snowballing type papers that are referenced either forward, backward, or from both directions are
     * fetched by calling the [fetcherManager].
     */
    private suspend fun runFetching(job: FetcherProcessingJob, paper: Paper): FetchingResults {
        val backwardReferences = if (job.snowballingType.isBackwardOrBoth()) {
            fetch(job.fetchers, paper, FetchingDirection.BACKWARD)
        } else {
            emptySet()
        }

        val forwardReferences = if (job.snowballingType.isForwardOrBoth()) {
            fetch(job.fetchers, paper, FetchingDirection.FORWARD)
        } else {
            emptySet()
        }

        return FetchingResults(backwardReferences, forwardReferences)
    }

    /**
     * Calls the [fetcherManager] to fetch papers according to the passed [direction] for each fetcher in [fetchers].
     *
     * If a [FetcherException] is thrown by one of the fetchers, the exception is caught and logged.
     */
    private suspend fun fetch(fetchers: FetcherMap, paper: Paper, direction: FetchingDirection): Set<FetcherPaper> {
        val fetchCall = when (direction) {
            FetchingDirection.BACKWARD -> fetcherManager::fetchBackwardReferences
            FetchingDirection.FORWARD -> fetcherManager::fetchForwardReferences
        }

        val set = mutableSetOf<FetcherPaper>()

        for ((fetcher, options) in fetchers) {
            try {
                val fetchedPapers = fetchCall(fetcher, paper.toFetcherPaper(), options)
                set += fetchedPapers
            } catch (ex: FetcherException) {
                logger.error(ex) {
                    "Failed to fetch ${direction.displayName} with fetcher '$fetcher': ${ex.message}"
                }
            }
        }
        logger.info {
            "Fetched ${set.size} ${direction.displayName} referenced papers from ${fetchers.size} fetcher(s) for " +
                "paper ${paper.id}"
        }

        return set
    }

    /**
     * Runs the paper creation part of the job processing.
     *
     * For both sets in [results] the paper data is stored in the DB if not already existent.
     */
    private suspend fun runPaperCreation(results: FetchingResults): PaperCreationResults {
        val createdBackwardRefs = createRefs(results.backwardRefs)
        logger.info { "Created ${createdBackwardRefs.size} backward referenced papers" }
        val createdForwardRefs = createRefs(results.forwardRefs)
        logger.info { "Created ${createdForwardRefs.size} forward referenced papers" }

        return PaperCreationResults(createdBackwardRefs, createdForwardRefs)
    }

    /**
     * Creates a DB paper for each of the [FetcherPaper]s in [refs].
     *
     * If a paper already exists in the DB, no paper is added and the existent paper is retrieved and added to the
     * result set.
     */
    private suspend fun createRefs(refs: Set<FetcherPaper>): Set<Paper> {
        val createdPaperRefs = mutableSetOf<Paper>()

        for (ref in refs) {
            if (ref.externalId != null) {
                // TODO: replace with check for whole paper data by similarity not only external ID
                val result = paperRepo.getPaperByExternalId(ref.externalId)
                if (result.isSuccess) {
                    // TODO: merge data
                    createdPaperRefs += result.getOrThrow()
                    continue
                }
            }

            try {
                createdPaperRefs += paperRepo.createPaper(ref.toGrpcPaperRequest())
            } catch (ex: SQLException) {
                logger.error(ex) { "Failed to create paper for fetched paper: ${ex.message}" }
            }
        }

        return createdPaperRefs
    }

    /**
     * Runs the citation part of the job processing.
     *
     * For both sets in [creationResults] citation entries are added for the origin paper with the passed [paperId] and
     * the reference in one of the sets.
     */
    private suspend fun runPaperCitation(paperId: UUID, creationResults: PaperCreationResults) {
        createCitation(paperId, creationResults.backwardRefs, FetchingDirection.BACKWARD)
        createCitation(paperId, creationResults.forwardRefs, FetchingDirection.FORWARD)
    }

    /**
     * Creates a citation entry for the paper with the passed [paperId] and each reference in [refs] according to the
     * [direction].
     *
     * If a citation entry already exists for a combination, nothing happens as this means the connection between both
     * papers already exists (no-op).
     */
    private suspend fun createCitation(paperId: UUID, refs: Set<Paper>, direction: FetchingDirection) {
        val citeCall = when (direction) {
            FetchingDirection.BACKWARD -> citationRepo::addBackwardReferencedPaper
            FetchingDirection.FORWARD -> citationRepo::addForwardReferencedPaper
        }
        val refName = "${direction.displayName} reference"

        var addedCitations = 0
        for (ref in refs) {
            try {
                citeCall(paperId, ref.id)
                addedCitations++
            } catch (ex: SQLException) {
                // A unique constraint violation is okay (no-op)
                if (!ex.isUniqueConstraintViolation()) {
                    logger.error(ex) {
                        "Failed to create $refName between paper $paperId and reference ${ref.id}: ${ex.message}"
                    }
                }
            }
        }

        logger.info { "Added $addedCitations ${refName}s for paper $paperId" }
    }

    /**
     * Runs the part of the job processing in which the created papers are added to the project.
     *
     * All papers in [creationResults] are added to the target stage in the project, if not already added. If the
     * maximum stage of the project is below the target stage, it will be increased. A paper that has already been added
     * to the project is a reference of a paper in an earlier stage.
     */
    private suspend fun runAddingPapersToProject(job: FetcherProcessingJob, creationResults: PaperCreationResults) {
        val baseRequest = GrpcProjectPaper.Add.newBuilder()
            .setProjectId(job.projectId.toString())
            .setStage(job.targetStage)

        val filteredRefs = mutableListOf<Paper>()
        for (createdRef in creationResults.allRefs) {
            val doesAlreadyExist = projectPaperRepo.doesProjectPaperExist(job.projectId, createdRef.id)
            if (doesAlreadyExist) {
                logger.debug {
                    "Skipping paper ${createdRef.id} because it is already added to the project"
                }
                continue
            }

            filteredRefs += createdRef
        }

        logger.debug {
            "Adding ${filteredRefs.size}/${creationResults.allRefs.size} papers to project ${job.projectId}"
        }

        if (filteredRefs.isNotEmpty()) {
            projectRepo.updateMaxStageIfExceeded(job.projectId, job.targetStage)
        }

        for (ref in filteredRefs) {
            try {
                val request = baseRequest.setPaperId(ref.id.toString()).build()
                projectPaperRepo.addPaperToProject(request, job.triggeringUserId)
            } catch (ex: SQLException) {
                logger.error(ex) {
                    "Failed to add paper ${ref.id} to project ${job.projectId} in stage ${job.targetStage}:" +
                        " ${ex.message}"
                }
            }
        }
    }
}
