package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.model.dto.toGrpcPaperRequest
import se.uulm.snowballr.backend.model.exception.FetcherException
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import snowballr.Fetcher.AvailableFetchers
import snowballr.Fetcher.FetcherOptions
import snowballr.Fetcher.GetAvailableFetcherOptionsRequest
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

private val logger = KotlinLogging.logger { }

interface IFetcherService {
    /**
     * Service implementation of [SnowballRService.getAvailableFetchers].
     */
    suspend fun getAvailableFetchers(): AvailableFetchers

    /**
     * Service implementation of [SnowballRService.getAvailableFetcherOptions].
     */
    suspend fun getAvailableFetcherOptions(request: GetAvailableFetcherOptionsRequest): FetcherOptions

    /**
     * Service implementation of [SnowballRService.searchLocalProjectPaperCandidates].
     */
    suspend fun searchLocalProjectPaperCandidates(request: GrpcProjectPaper.SearchQuery): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.searchFetcherProjectPaperCandidates].
     */
    suspend fun searchFetcherProjectPaperCandidates(request: GrpcProjectPaper.SearchQuery): GrpcPaper.List
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
    override suspend fun getAvailableFetchers(): AvailableFetchers = AvailableFetchers
        .newBuilder()
        .addAllFetcherNames(fetcherManager.getAvailableFetchers())
        .build()

    override suspend fun getAvailableFetcherOptions(request: GetAvailableFetcherOptionsRequest): FetcherOptions =
        FetcherOptions.newBuilder()
            .putAllOptions(fetcherManager.getAvailableOptions(request.fetcherName))
            .build()

    override suspend fun searchLocalProjectPaperCandidates(request: GrpcProjectPaper.SearchQuery): GrpcPaper.List =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)

            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val matchingPapers = paperRepo.getPapersBySearchQuery(request.query)
            logger.debug { "Found ${matchingPapers.size} papers for query '${request.query}'" }

            val filteredPapers = filterPapersNotInProject(projectId, matchingPapers)
            logger.debug {
                val removedPapers = matchingPapers.size - filteredPapers.size
                "Filtered out $removedPapers/${matchingPapers.size} papers that already existed in the project."
            }

            GrpcPaper.List.newBuilder()
                .addAllPapers(filteredPapers.map { it.toGrpcPaper(emptyList()) })
                .build()
        }

    override suspend fun searchFetcherProjectPaperCandidates(request: GrpcProjectPaper.SearchQuery): GrpcPaper.List =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)

            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)
            val project = projectRepo.getProjectById(projectId).getOrThrow()

            val papers = mutableSetOf<FetcherPaper>()
            for ((fetcher, options) in project.fetchers) {
                try {
                    papers += fetcherManager.searchPapers(fetcher, request.query, options)
                } catch (e: FetcherException) {
                    logger.error(e) { "Failed to search fetcher papers for fetcher '$fetcher': ${e.message}" }
                }
            }
            logger.debug { "Found ${papers.size} papers for query '${request.query}'" }

            val filteredPapers = filterExistingFetcherPapers(projectId, papers)
            logger.debug {
                val removedPapers = papers.size - filteredPapers.size
                "Filtered out $removedPapers/${papers.size} papers that already existed in the project."
            }

            GrpcPaper.List.newBuilder()
                .addAllPapers(filteredPapers)
                .build()
        }

    /**
     * Filters out papers that are already added to the project.
     *
     * Papers that already exist in the database get their ID assigned, so that they can be associated by the client.
     */
    private suspend fun filterExistingFetcherPapers(projectId: UUID, fetcherPapers: Set<FetcherPaper>): Set<GrpcPaper> {
        val externalIds = fetcherPapers.mapNotNull { it.externalId }
        val existingPapers = paperRepo.getPapersByExternalIds(externalIds).associateBy { it.externalId }
        val notInProjectIds = filterPapersNotInProject(projectId, existingPapers.values).map { it.id }

        return fetcherPapers.mapNotNull { fetcherPaper ->
            val existing = existingPapers[fetcherPaper.externalId]
            when {
                existing == null -> fetcherPaper.toGrpcPaperRequest()
                existing.id in notInProjectIds -> existing.toGrpcPaper(emptyList())
                else -> null
            }
        }.toSet()
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
