package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.PaperNavigationDirection
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithReviewsCount
import se.uulm.snowballr.backend.model.dto.hasNoFinalDecision
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPaper
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPapers
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateProjectPaperException
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException
import se.uulm.snowballr.backend.model.exception.invalidargument.StageOutOfRangeException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.accessrules.IAccessChecker
import se.uulm.snowballr.backend.service.accessrules.checkFor
import snowballr.ProjectOuterClass.Project
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper
import snowballr.ReviewOuterClass.Review as GrpcReview

interface IProjectPaperService {
    /**
     * Service implementation of [SnowballRService.getProjectPaperById].
     */
    suspend fun getProjectPaperById(projectPaperId: UUID): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getProjectPaperByRelativeId].
     */
    suspend fun getProjectPaperByRelativeId(request: GrpcProjectPaper.Get): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getAllProjectPapersForProject].
     */
    suspend fun getAllProjectPapersForProject(projectId: UUID): GrpcProjectPaper.List

    /**
     * Service implementation of [SnowballRService.getPapersToReviewForProject].
     */
    suspend fun getPapersToReviewForProject(projectId: UUID): GrpcProjectPaper.List

    /**
     * Service implementation of [SnowballRService.addPaperToProject].
     */
    suspend fun addPaperToProject(request: GrpcProjectPaper.Add): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getNextPaper].
     */
    suspend fun getNextPaper(projectPaperId: UUID): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getPreviousPaper].
     */
    suspend fun getPreviousPaper(projectPaperId: UUID): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getNextPaperToReview].
     */
    suspend fun getNextPaperToReview(projectPaperId: UUID): GrpcProjectPaper
}

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
 * @param accessChecker Interface for checking access permissions based on defined rules.
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
    private val accessChecker: IAccessChecker,
) : IProjectPaperService {
    override suspend fun getProjectPaperById(projectPaperId: UUID): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

            accessChecker.isAllowedToReadProject().checkFor(currentUser, projectPaper.projectId)

            projectPaper.toGrpcProjectPaperWithData()
        }

    override suspend fun getProjectPaperByRelativeId(request: GrpcProjectPaper.Get): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)

            accessChecker.isAllowedToReadProject().checkFor(currentUser, projectId)

            val relativeId = request.relativeProjectPaperId.toLong()
            val projectPaper = repo.getProjectPaperByRelativeId(projectId, relativeId).getOrThrow()

            projectPaper.toGrpcProjectPaperWithData()
        }

    override suspend fun getAllProjectPapersForProject(projectId: UUID): GrpcProjectPaper.List =
        getProjectPapers(projectId)

    override suspend fun getPapersToReviewForProject(projectId: UUID): GrpcProjectPaper.List {
        val predicate: (ProjectPaperWithPaper, Map<ProjectPaper, List<GrpcReview>>, String) -> Boolean =
            { projectPaper, projectPaperReviewsMap, currentUserId ->
                val isAlreadyReviewedByCurrentUser = projectPaperReviewsMap[projectPaper.projectPaper]
                    ?.any { review -> review.userId == currentUserId } == true

                !isAlreadyReviewedByCurrentUser && projectPaper.hasNoFinalDecision()
            }
        return getProjectPapers(projectId, predicate)
    }

    override suspend fun addPaperToProject(request: GrpcProjectPaper.Add): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)
            val paperId = parseUUID(request.paperId, EntityType.PAPER)

            accessChecker.isProjectOrServerAdmin(AccessType.CREATE).checkFor(currentUser, projectId)

            val project = projectRepo.getProjectById(projectId).getOrThrow()
            accessChecker.isProjectActive().checkFor(currentUser, project)

            val paper = paperRepo.getPaperById(paperId).getOrThrow()
            if (repo.doesProjectPaperExist(projectId, paperId)) {
                throw DuplicateProjectPaperException(projectId, paperId)
            }

            if (request.stage !in 0..project.maxStage) {
                throw StageOutOfRangeException(request.stage, project.maxStage)
            }

            val projectPaper = repo.addPaperToProject(request, currentUser.id)

            projectPaper.toGrpcProjectPaperWithData(paper)
        }

    override suspend fun getNextPaper(projectPaperId: UUID): GrpcProjectPaper =
        getAdjacentPaper(projectPaperId, PaperNavigationDirection.NEXT)

    override suspend fun getPreviousPaper(projectPaperId: UUID): GrpcProjectPaper =
        getAdjacentPaper(projectPaperId, PaperNavigationDirection.PREVIOUS)

    override suspend fun getNextPaperToReview(projectPaperId: UUID): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()
            val projectId = projectPaper.projectId

            accessChecker.isAllowedToReadProject().checkFor(currentUser, projectId)

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

            (papersWithoutFinalDecision.firstOrNull() ?: sortedPapers.first()).toGrpcProjectPaperWithData()
        }

    /**
     * Populates the given [ProjectPaper] with its authors, backward references, and reviews.
     *
     * @param associatedPaper The [Paper] that is associated with this [ProjectPaper]. If not provided, it is requested
     * from the database using the paper id from the project paper.
     * @return The gRPC representation of the project paper, including the associated data.
     */
    private suspend fun ProjectPaper.toGrpcProjectPaperWithData(associatedPaper: Paper? = null): GrpcProjectPaper {
        val paper = associatedPaper ?: paperRepo.getPaperById(paperId).getOrThrow()

        val backwardReferences = citationTableRepo
            .getBackwardsReferencedPaperIdsOfPaperById(paper.id).map(UUID::toString)

        val reviews = reviewTableRepo
            .getAllReviewsForProjectPaper(id)
            .map {
                val selectedCriteriaIds = reviewHasCriterionTableRepo.getSelectedCriteriaIdsForReviewById(it.id)
                it.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
            }

        return ProjectPaperWithPaper(this, paper).toGrpcProjectPaper(backwardReferences, reviews)
    }

    /**
     * Retrieves a list of [ProjectPaper] associated with a specified project id. This method also ensures access
     * control for the current user. Optionally, a predicate function can be provided to filter the [ProjectPaper]s
     * based on custom criteria.
     *
     * @param projectId The ID of the [Project] for which [ProjectPaper]s are to be retrieved.
     * @param predicate An optional lambda function that takes a [ProjectPaperWithPaper], a map of [GrpcReview], and a
     * user ID. This function should return a boolean value to filter the [ProjectPaperWithPaper]s. If null, no
     * filtering is applied.
     * @return A list of [GrpcProjectPaper] including associated metadata such as authors, backward references, and
     * reviews.
     * @throws UnauthorizedException If the user does not have the required access to the project.
     */
    private suspend fun getProjectPapers(
        projectId: UUID,
        predicate: (suspend (ProjectPaperWithPaper, Map<ProjectPaper, List<GrpcReview>>, String) -> Boolean)? = null,
    ): GrpcProjectPaper.List = withUser(userRepo) { currentUser ->
        accessChecker.isAllowedToReadProject().checkFor(currentUser, projectId)

        var projectPapersWithPapers = repo.getAllProjectPapersWithPapers(projectId)
        val paperBackwardReferencesMap = mutableMapOf<Paper, List<String>>()
        val projectPaperReviewsMap = mutableMapOf<ProjectPaper, List<GrpcReview>>()

        for (projectPaper in projectPapersWithPapers) {
            val paper = projectPaper.paper
            paperBackwardReferencesMap[paper] = citationTableRepo
                .getBackwardsReferencedPaperIdsOfPaperById(paper.id).map(UUID::toString)
            projectPaperReviewsMap[projectPaper.projectPaper] = reviewTableRepo
                .getAllReviewsForProjectPaper(projectPaper.projectPaper.id)
                .map {
                    val selectedCriteriaIds = reviewHasCriterionTableRepo.getSelectedCriteriaIdsForReviewById(it.id)
                    it.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
                }
        }

        projectPapersWithPapers = predicate?.let { pred ->
            projectPapersWithPapers.filter { pred(it, projectPaperReviewsMap, currentUser.id.toString()) }
        } ?: projectPapersWithPapers

        projectPapersWithPapers.toGrpcProjectPapers(paperBackwardReferencesMap, projectPaperReviewsMap)
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
    private suspend fun getAdjacentPaper(projectPaperId: UUID, direction: PaperNavigationDirection): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

            accessChecker.isAllowedToReadProject().checkFor(currentUser, projectPaper.projectId)

            val projectId = projectRepo.getProjectById(projectPaper.projectId).getOrThrow().id
            val adjacentPaper = repo.getAdjacentPaper(projectId, projectPaper.localPaperId, direction).getOrThrow()
            val paper = paperRepo.getPaperById(adjacentPaper.paperId).getOrThrow()

            adjacentPaper.toGrpcProjectPaperWithData(paper)
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
