package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IProjectPaperAccessChecker
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.PaperNavigationDirection
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaper
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperWithReviewsCount
import se.uulm.snowballr.backend.model.dto.projectpaper.hasNoFinalDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.toProjectPaperResponses
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateProjectPaperException
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException
import se.uulm.snowballr.backend.model.exception.invalidargument.StageOutOfRangeException
import se.uulm.snowballr.backend.model.outgoing.projectpaper.ProjectPaperResponse
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import java.util.UUID

interface IProjectPaperService {
    /**
     * Service implementation of [SnowballRService.getProjectPaperById].
     */
    suspend fun getProjectPaperById(projectPaperId: UUID): ProjectPaperResponse

    /**
     * Service implementation of [SnowballRService.getProjectPaperByRelativeId].
     */
    suspend fun getProjectPaperByRelativeId(projectId: UUID, relativeId: Int): ProjectPaperResponse

    /**
     * Service implementation of [SnowballRService.getAllProjectPapersForProject].
     */
    suspend fun getAllProjectPapersForProject(projectId: UUID): List<ProjectPaperResponse>

    /**
     * Service implementation of [SnowballRService.getPapersToReviewForProject].
     */
    suspend fun getPapersToReviewForProject(projectId: UUID): List<ProjectPaperResponse>

    /**
     * Service implementation of [SnowballRService.addPaperToProject].
     */
    suspend fun addPaperToProject(projectId: UUID, paperId: UUID, stage: Int): ProjectPaperResponse

    /**
     * Service implementation of [SnowballRService.getNextPaper].
     */
    suspend fun getNextPaper(projectPaperId: UUID): ProjectPaperResponse

    /**
     * Service implementation of [SnowballRService.getPreviousPaper].
     */
    suspend fun getPreviousPaper(projectPaperId: UUID): ProjectPaperResponse

    /**
     * Service implementation of [SnowballRService.getNextPaperToReview].
     */
    suspend fun getNextPaperToReview(projectPaperId: UUID): ProjectPaperResponse
}

private typealias ProjectPaperFilter =
    (suspend (ProjectPaperWithPaper, Map<ProjectPaper, List<ReviewResponse>>, UUID) -> Boolean)

/**
 * The [ProjectPaperService] class handles operations related to project papers by implementing the
 * [IProjectPaperService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of project paper CRUD operations,
 * delegating the actual persistence operations to the [IProjectPaperTableRepo] repository.
 *
 * @constructor Initializes the [ProjectPaperService] with a project paper repository.
 * @param repo The repository responsible for managing persistence operations for project papers.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectRepo The repository responsible for managing persistence operations for projects.
 * @param paperRepo The repository responsible for managing persistence operations for papers.
 * @param citationTableRepo The repository responsible for managing persistence operations for the citation relation.
 * @param reviewTableRepo The repository responsible for managing persistence operations for the reviews
 * @param reviewHasCriterionTableRepo The repository responsible for managing persistence operations for the review has
 * @param accessChecker Interface for checking access permissions for project papers based on defined rules.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class ProjectPaperService(
    private val repo: IProjectPaperTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val citationTableRepo: ICitationTableRepo,
    private val reviewTableRepo: IReviewTableRepo,
    private val reviewHasCriterionTableRepo: IReviewHasCriterionTableRepo,
    private val accessChecker: IProjectPaperAccessChecker,
    private val projectAccessChecker: IProjectAccessChecker,
) : IProjectPaperService {
    override suspend fun getProjectPaperById(projectPaperId: UUID): ProjectPaperResponse =
        withUser(userRepo) { currentUser ->
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

            projectAccessChecker.isAllowedToReadProject(currentUser, projectPaper.projectId)

            projectPaper.toProjectPaperResponse()
        }

    override suspend fun getProjectPaperByRelativeId(projectId: UUID, relativeId: Int): ProjectPaperResponse =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val projectPaper = repo.getProjectPaperByRelativeId(projectId, relativeId).getOrThrow()

            projectPaper.toProjectPaperResponse()
        }

    override suspend fun getAllProjectPapersForProject(projectId: UUID): List<ProjectPaperResponse> =
        getProjectPapers(projectId)

    override suspend fun getPapersToReviewForProject(projectId: UUID): List<ProjectPaperResponse> {
        val predicate: (ProjectPaperWithPaper, Map<ProjectPaper, List<ReviewResponse>>, UUID) -> Boolean =
            { projectPaper, projectPaperReviewsMap, currentUserId ->
                val isAlreadyReviewedByCurrentUser = projectPaperReviewsMap[projectPaper.projectPaper]
                    ?.any { review -> review.userId == currentUserId } == true

                !isAlreadyReviewedByCurrentUser && projectPaper.hasNoFinalDecision()
            }
        return getProjectPapers(projectId, predicate)
    }

    override suspend fun addPaperToProject(projectId: UUID, paperId: UUID, stage: Int): ProjectPaperResponse =
        withUser(userRepo) { currentUser ->
            val projectResult = projectRepo.getProjectById(projectId)
            accessChecker.isAllowedToAddPaperToProject(currentUser, projectId, projectResult)
            val project = projectResult.getOrThrow()

            val paper = paperRepo.getPaperById(paperId).getOrThrow()
            if (repo.doesProjectPaperExist(projectId, paperId)) {
                throw DuplicateProjectPaperException(projectId, paperId)
            }

            if (stage !in 0..project.maxStage) {
                throw StageOutOfRangeException(stage, project.maxStage)
            }

            val projectPaper = repo.addPaperToProject(projectId, paperId, stage, currentUser.id)

            projectPaper.toProjectPaperResponse(paper)
        }

    override suspend fun getNextPaper(projectPaperId: UUID): ProjectPaperResponse =
        getAdjacentPaper(projectPaperId, PaperNavigationDirection.NEXT)

    override suspend fun getPreviousPaper(projectPaperId: UUID): ProjectPaperResponse =
        getAdjacentPaper(projectPaperId, PaperNavigationDirection.PREVIOUS)

    override suspend fun getNextPaperToReview(projectPaperId: UUID): ProjectPaperResponse =
        withUser(userRepo) { currentUser ->
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()
            val projectId = projectPaper.projectId

            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val projectPapers =
                repo.getSubsequentProjectPapers(projectId, projectPaper.localPaperId, projectPaper.stage)
            val projectPapersWithReviewsCount = getProjectPapersWithReviewsCount(projectPapers, currentUser.id)

            if (projectPapersWithReviewsCount.isEmpty()) {
                throw FailedPreconditionException(
                    "There is no next project paper available to review in the project with ID $projectId",
                )
            }

            val sortedPapers = sortPapersByStageAndReviewsCount(projectPapersWithReviewsCount)
            val papersWithoutFinalDecision = sortedPapers.filter(ProjectPaper::hasNoFinalDecision)

            (papersWithoutFinalDecision.firstOrNull() ?: sortedPapers.first()).toProjectPaperResponse()
        }

    /**
     * Populates the given [ProjectPaper] with its backward references, and reviews.
     *
     * @param associatedPaper The [Paper] that is associated with this [ProjectPaper]. If not provided, it is requested
     * from the database using the paper id from the project paper.
     * @return The response representation of the project paper, including the associated data.
     */
    private suspend fun ProjectPaper.toProjectPaperResponse(associatedPaper: Paper? = null): ProjectPaperResponse {
        val paper = associatedPaper ?: paperRepo.getPaperById(paperId).getOrThrow()
        val paperResponse = paper.toPaperResponse(citationTableRepo)

        val reviews = reviewTableRepo
            .getAllReviewsForProjectPaper(id)
            .map {
                val selectedCriteriaIds = reviewHasCriterionTableRepo.getSelectedCriteriaIdsForReviewById(it.id)
                ReviewResponse.fromReviewAndIds(it, selectedCriteriaIds)
            }

        return ProjectPaperResponse(
            id = this.id,
            stage = this.stage,
            decision = this.decision,
            localPaperId = this.localPaperId,
            paper = paperResponse,
            reviews = reviews,
        )
    }

    /**
     * Retrieves a list of [ProjectPaper] associated with a specified project id. This method also ensures access
     * control for the current user. Optionally, a predicate function can be provided to filter the [ProjectPaper]s
     * based on custom criteria.
     *
     * @param projectId The ID of the [Project] for which [ProjectPaper]s are to be retrieved.
     * @param predicate An optional lambda function that takes a [ProjectPaperWithPaper], a map of [ReviewResponse],
     * and a user ID. This function should return a boolean value to filter the [ProjectPaperWithPaper]s. If null, no
     * filtering is applied.
     * @return A list of [ProjectPaperResponse] including associated metadata such as backward references, and reviews.
     * @throws UnauthorizedException If the user does not have the required access to the project.
     */
    private suspend fun getProjectPapers(
        projectId: UUID,
        predicate: ProjectPaperFilter? = null,
    ): List<ProjectPaperResponse> = withUser(userRepo) { currentUser ->
        projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

        var projectPapersWithPapers = repo.getAllProjectPapersWithPapers(projectId)
        val paperBackwardReferencesMap = mutableMapOf<Paper, List<UUID>>()
        val projectPaperReviewsMap = mutableMapOf<ProjectPaper, List<ReviewResponse>>()

        for (projectPaper in projectPapersWithPapers) {
            val paper = projectPaper.paper
            paperBackwardReferencesMap[paper] = citationTableRepo.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            projectPaperReviewsMap[projectPaper.projectPaper] = reviewTableRepo
                .getAllReviewsForProjectPaper(projectPaper.projectPaper.id)
                .map {
                    val selectedCriteriaIds = reviewHasCriterionTableRepo.getSelectedCriteriaIdsForReviewById(it.id)
                    ReviewResponse.fromReviewAndIds(it, selectedCriteriaIds)
                }
        }

        projectPapersWithPapers = predicate?.let { pred ->
            projectPapersWithPapers.filter { pred(it, projectPaperReviewsMap, currentUser.id) }
        } ?: projectPapersWithPapers

        projectPapersWithPapers.toProjectPaperResponses(paperBackwardReferencesMap, projectPaperReviewsMap)
    }

    /**
     * Retrieves the following project paper based on the provided project paper ID and the following paper computation
     * logic.
     *
     * @param projectPaperId The unique identifier of the current project paper.
     * @param direction The navigation direction indicating whether to retrieve the next or previous paper.
     * Containing the relative ID of the following paper.
     * @return The gRPC representation of the following project paper, including its associated metadata.
     * @throws NotFoundException If the project, project paper, or associated paper cannot be found.
     * @throws InvalidUUIDException If the given project paper ID is not a valid UUID.
     * @throws UnauthorizedException If the user does not have the required access to the project.
     */
    private suspend fun getAdjacentPaper(
        projectPaperId: UUID,
        direction: PaperNavigationDirection,
    ): ProjectPaperResponse = withUser(userRepo) { currentUser ->
        val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

        projectAccessChecker.isAllowedToReadProject(currentUser, projectPaper.projectId)

        val projectId = projectRepo.getProjectById(projectPaper.projectId).getOrThrow().id
        val adjacentPaper = repo.getAdjacentPaper(projectId, projectPaper.localPaperId, direction).getOrThrow()
        val paper = paperRepo.getPaperById(adjacentPaper.paperId).getOrThrow()

        adjacentPaper.toProjectPaperResponse(paper)
    }

    /**
     * Sorts a list of project papers paired with their corresponding review counts.
     *
     * The result only contains the sorted project papers, excluding the review counts.
     *
     * @param projectPapersWithReviewsCount A list of [ProjectPaperWithReviewsCount] objects to be sorted.
     * @return A list of [ProjectPaper] objects sorted by stage (ascending) and reviews count (ascending).
     */
    private fun sortPapersByStageAndReviewsCount(projectPapersWithReviewsCount: List<ProjectPaperWithReviewsCount>) =
        projectPapersWithReviewsCount.sorted().map { it.projectPaper }

    private suspend fun getProjectPapersWithReviewsCount(projectPapers: List<ProjectPaper>, currentUserId: UUID) =
        projectPapers
            .map { projectPaper ->
                val reviews = reviewTableRepo.getAllReviewsForProjectPaper(projectPaper.id)

                val hasOwnReview = reviews.any { it.userId == currentUserId }
                if (hasOwnReview) {
                    ProjectPaperWithReviewsCount(projectPaper, -1)
                } else {
                    ProjectPaperWithReviewsCount(projectPaper, reviews.size)
                }
            }
            .filter { it.reviewsCount >= 0 }
}
