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
import se.uulm.snowballr.backend.matching.IPaperMatcher
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper
import se.uulm.snowballr.backend.model.exception.FetcherException
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.model.fetcher.FetcherProcessingJob
import se.uulm.snowballr.backend.model.fetcher.FetchingDirection
import se.uulm.snowballr.backend.model.fetcher.FetchingResults
import se.uulm.snowballr.backend.model.fetcher.PaperCreationResults
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.isUniqueConstraintViolation
import java.sql.SQLException
import java.util.UUID

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

@Suppress("LongParameterList")
class FetcherOrchestrator(
    private val fetcherManager: IFetcherManager,
    private val projectRepo: IProjectTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val paperMatcher: IPaperMatcher,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
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
                    logger.error(e) { "Failed to process job: ${e.message ?: "<empty>"}" }
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
            similarityThreshold = project.similarityThreshold,
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

        val dedupResults = runDeduplication(fetchingResults, job)

        val creationResults = runPaperCreation(dedupResults, job)

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
        val backwardReferences = if (job.snowballingType.isBackwardOrBoth) {
            fetch(job.fetchers, paper, FetchingDirection.BACKWARD)
        } else {
            emptySet()
        }

        val forwardReferences = if (job.snowballingType.isForwardOrBoth) {
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
                    "Failed to fetch ${direction.displayName} with fetcher '$fetcher': ${ex.message ?: "<empty>"}"
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
     * Deduplicates the results from the fetching part of the processing job.
     */
    private fun runDeduplication(results: FetchingResults, job: FetcherProcessingJob): FetchingResults {
        val dedupedBackward = paperMatcher.deduplicatePapers(results.backwardRefs, job.similarityThreshold)
        val dedupedForward = paperMatcher.deduplicatePapers(results.forwardRefs, job.similarityThreshold)

        return FetchingResults(dedupedBackward, dedupedForward)
    }

    /**
     * Runs the paper creation part of the job processing.
     *
     * For both sets in [results] the paper data is stored in the DB if not already existent.
     */
    private suspend fun runPaperCreation(results: FetchingResults, job: FetcherProcessingJob): PaperCreationResults {
        val createdBackwardRefs = createRefs(results.backwardRefs, job.similarityThreshold)
        logger.info { "Created ${createdBackwardRefs.size} backward referenced papers" }
        val createdForwardRefs = createRefs(results.forwardRefs, job.similarityThreshold)
        logger.info { "Created ${createdForwardRefs.size} forward referenced papers" }

        return PaperCreationResults(createdBackwardRefs, createdForwardRefs)
    }

    /**
     * Resolves each [FetcherPaper] in [refs] against the DB:
     * 1. External-ID fast path — if an exact match exists, merge metadata and reuse it.
     * 2. Year-filtered similarity fallback — if a similar paper is found, merge metadata and reuse it.
     * 3. No match — create a new paper.
     */
    private suspend fun createRefs(refs: Set<FetcherPaper>, threshold: Float): Set<Paper> {
        val result = mutableSetOf<Paper>()
        for (ref in refs) {
            val paper = resolveExistingPaper(ref, threshold) ?: createNewPaper(ref)
            if (paper != null) result += paper
        }
        return result
    }

    private suspend fun resolveExistingPaper(ref: FetcherPaper, threshold: Float): Paper? {
        if (ref.externalIds.isNotEmpty()) {
            val existingPapers = paperRepo.getPapersByExternalIds(ref.externalIds)

            if (existingPapers.isNotEmpty()) {
                if (existingPapers.size > 1) {
                    logger.error {
                        "Several papers existing for external IDs (${ref.externalIds}) of single fetcher paper"
                    }
                }

                val paper = existingPapers.first()
                updateMetadataIfChanged(paper, ref)
                return paper
            }
        }

        val candidates = paperRepo.getPapersByYear(ref.year, paperMatcher.config.yearTolerance)
        val match = if (candidates.isEmpty()) null else paperMatcher.findMatch(ref, candidates, threshold)
        if (match != null) {
            updateMetadataIfChanged(match, ref)
            return match
        }

        return null
    }

    private suspend fun updateMetadataIfChanged(dbPaper: Paper, ref: FetcherPaper) {
        val mergedMeta = paperMatcher.mergeMetadata(dbPaper, ref)
        if (mergedMeta == dbPaper.fetcherMetadata) return
        try {
            paperRepo.updateFetcherMetadata(dbPaper.id, mergedMeta)
        } catch (ex: SQLException) {
            logger.error(ex) {
                "Failed to update fetcher metadata for paper ${dbPaper.id}: ${ex.message ?: "<empty>"}"
            }
        }
    }

    private suspend fun createNewPaper(ref: FetcherPaper): Paper? = try {
        paperRepo.createPaper(CreatePaperRequest.fromFetcherPaper(ref))
    } catch (ex: SQLException) {
        logger.error(ex) { "Failed to create paper for fetched paper: ${ex.message ?: "<empty>"}" }
        null
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
                        "Failed to create $refName between paper $paperId and reference ${ref.id}: " +
                            (ex.message ?: "<empty>")
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
        val filteredRefs = mutableListOf<Paper>()
        // TODO: don't use creation result, re-fetch all refs to also include existing refs
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
                projectPaperRepo.addPaperToProject(job.projectId, ref.id, job.targetStage, job.triggeringUserId)
            } catch (ex: SQLException) {
                logger.error(ex) {
                    "Failed to add paper ${ref.id} to project ${job.projectId} in stage ${job.targetStage}: " +
                        (ex.message ?: "<empty>")
                }
            }
        }
    }
}
