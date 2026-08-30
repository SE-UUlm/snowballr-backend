package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.exception.FetcherException
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.model.outgoing.paper.FetcherPaperResponse
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import java.util.UUID

private val logger = KotlinLogging.logger { }

interface IFetcherService {
    /**
     * Service implementation of [SnowballRService.getAvailableFetchers].
     */
    suspend fun getAvailableFetchers(): Set<FetcherInformationWithId>

    /**
     * Service implementation of [SnowballRService.searchLocalProjectPaperCandidates].
     */
    suspend fun searchLocalProjectPaperCandidates(projectId: UUID, query: String): List<PaperResponse>

    /**
     * Service implementation of [SnowballRService.searchFetcherProjectPaperCandidates].
     */
    suspend fun searchFetcherProjectPaperCandidates(projectId: UUID, query: String): List<FetcherPaperResponse>
}

/**
 * Handles operations related to fetchers, fetcher options, and fetcher papers.
 *
 * @param fetcherManager The [IFetcherManager] that manages the available fetchers.
 * @param projectRepo The repository responsible for managing persistence operations for projects.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 * @param paperRepo The repository responsible for managing persistence operations for papers.
 * @param projectPaperRepo Repository interface to manage operations related to project papers.
 */
class FetcherService(
    private val fetcherManager: IFetcherManager,
    private val projectRepo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectAccessChecker: IProjectAccessChecker,
    private val paperRepo: IPaperTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
) : IFetcherService {
    override suspend fun getAvailableFetchers(): Set<FetcherInformationWithId> = fetcherManager.getAvailableFetchers()

    override suspend fun searchLocalProjectPaperCandidates(projectId: UUID, query: String): List<PaperResponse> =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val matchingPapers = paperRepo.getPapersBySearchQuery(query)
            logger.debug { "Found ${matchingPapers.size} papers for query '$query'" }

            val filteredPapers = filterPapersNotInProject(projectId, matchingPapers)
            logger.debug {
                val removedPapers = matchingPapers.size - filteredPapers.size
                "Filtered out $removedPapers/${matchingPapers.size} papers that already existed in the project."
            }

            filteredPapers.map { PaperResponse.fromPaper(it, emptyList()) }
        }

    override suspend fun searchFetcherProjectPaperCandidates(
        projectId: UUID,
        query: String,
    ): List<FetcherPaperResponse> = withUser(userRepo) { currentUser ->
        projectAccessChecker.isAllowedToReadProject(currentUser, projectId)
        val project = projectRepo.getProjectById(projectId).getOrThrow()

        val papers = mutableSetOf<FetcherPaper>()
        for ((fetcher, options) in project.settings.fetchers) {
            try {
                papers += fetcherManager.searchPapers(fetcher, query, options)
            } catch (e: FetcherException) {
                logger.error(e) {
                    "Failed to search fetcher papers for fetcher '$fetcher': ${e.message ?: "<empty>"}"
                }
            }
        }
        logger.debug { "Found ${papers.size} papers for query '$query'" }

        val filteredPapers = filterExistingFetcherPapers(projectId, papers)
        logger.debug {
            val removedPapers = papers.size - filteredPapers.size
            "Filtered out $removedPapers/${papers.size} papers that already existed in the project."
        }

        filteredPapers
    }

    /**
     * Filters out papers that are already added to the project.
     *
     * Papers that already exist in the database get their ID assigned, so that they can be associated by the client.
     */
    private suspend fun filterExistingFetcherPapers(
        projectId: UUID,
        fetcherPapers: Set<FetcherPaper>,
    ): List<FetcherPaperResponse> {
        val externalIds = fetcherPapers.flatMap { it.externalIds }.distinct()
        val existingPapers = paperRepo.getPapersByExternalIds(externalIds)
        val notInProjectIds = filterPapersNotInProject(projectId, existingPapers).map { it.id }.toSet()

        val existingByExternalId = existingPapers.flatMap { paper -> paper.externalIds.map { it to paper } }.toMap()
        val getExisting = { fetcherPaper: FetcherPaper ->
            fetcherPaper.externalIds.firstNotNullOfOrNull { existingByExternalId[it] }
        }

        return fetcherPapers.mapNotNull { fetcherPaper ->
            val existing = getExisting(fetcherPaper)
            when {
                existing == null -> FetcherPaperResponse.fromFetcherPaper(fetcherPaper)
                existing.id in notInProjectIds -> FetcherPaperResponse.fromPaper(existing)
                else -> null
            }
        }
    }

    private suspend fun filterPapersNotInProject(projectId: UUID, papers: Collection<Paper>): List<Paper> {
        val filteredPapers = mutableSetOf<Paper>()

        for (paper in papers) {
            val isAlreadyInProject = projectPaperRepo.doesProjectPaperExist(projectId, paper.id)
            if (!isAlreadyInProject) {
                filteredPapers += paper
            }
        }

        return filteredPapers.toList()
    }
}
