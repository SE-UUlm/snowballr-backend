package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.PaperNavigationDirection
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.OutOfRangeException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPaper
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPapers
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.accessrules.andAlso
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import se.uulm.snowballr.backend.service.accessrules.isProjectActive
import se.uulm.snowballr.backend.service.accessrules.isProjectExistent
import se.uulm.snowballr.backend.service.accessrules.isServerOrProjectAdmin
import snowballr.Base
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.Project
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper
import snowballr.ReviewOuterClass.Review as GrpcReview

interface IProjectPaperService {
    /**
     * Service implementation of [SnowballRService.getProjectPaperById].
     */
    suspend fun getProjectPaperById(request: Base.Id): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getProjectPaperByRelativeId].
     */
    suspend fun getProjectPaperByRelativeId(request: GrpcProjectPaper.Get): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getAllProjectPapersForProject].
     */
    suspend fun getAllProjectPapersForProject(request: Base.Id): GrpcProjectPaper.List

    /**
     * Service implementation of [SnowballRService.getPapersToReviewForProject].
     */
    suspend fun getPapersToReviewForProject(request: Base.Id): GrpcProjectPaper.List

    /**
     * Service implementation of [SnowballRService.addPaperToProject].
     */
    suspend fun addPaperToProject(request: GrpcProjectPaper.Add): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getNextPaper].
     */
    suspend fun getNextPaper(request: Base.Id): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getPreviousPaper].
     */
    suspend fun getPreviousPaper(request: Base.Id): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getNextPaperToReview].
     */
    suspend fun getNextPaperToReview(request: Base.Id): GrpcProjectPaper
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
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 * @param citationTableRepo The repository responsible for managing persistence operations for the citation relation.
 * @param reviewTableRepo The repository responsible for managing persistence operations for the reviews
 * @param reviewHasCriterionTableRepo The repository responsible for managing persistence operations for the review has
 */
@Suppress("LongParameterList", "TooManyFunctions")
class ProjectPaperService(
    private val repo: IProjectPaperTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val citationTableRepo: ICitationTableRepo,
    private val reviewTableRepo: IReviewTableRepo,
    private val reviewHasCriterionTableRepo: IReviewHasCriterionTableRepo,
) : IProjectPaperService {
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
     * @param request The request containing the ID of the [Project] for which [ProjectPaper]s are to be retrieved.
     * @param predicate An optional lambda function that takes a [ProjectPaperWithPaper], a map of [GrpcReview], and a
     * user ID. This function should return a boolean value to filter the [ProjectPaperWithPaper]s. If null, no
     * filtering is applied.
     * @return A list of [GrpcProjectPaper] including associated metadata such as authors, backward references, and
     * reviews.
     * @throws UnauthorizedException If the user does not have the required access to the project.
     */
    private suspend fun getProjectPapers(
        request: Base.Id,
        predicate: (suspend (ProjectPaperWithPaper, Map<ProjectPaper, List<GrpcReview>>, String) -> Boolean)? = null,
    ): GrpcProjectPaper.List = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        isAllowedToReadProject(projectMemberRepo)
            .andAlso(isProjectExistent(projectRepo))
            .checkFor(currentUser, projectId)

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
     * @param id The unique identifier of the current project paper as a string.
     * @param direction The navigation direction indicating whether to retrieve the next or previous paper.
     * Containing the relative ID of the following paper.
     * @return The gRPC representation of the following project paper, including its associated metadata.
     * @throws NotFoundException If the project, project paper, or associated paper cannot be found.
     * @throws InvalidIdException.UUID If the given project paper ID is not a valid UUID.
     * @throws UnauthorizedException If the user does not have the required access to the project.
     */
    private suspend fun getAdjacentPaper(id: String, direction: PaperNavigationDirection): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectPaperId = parseUUID(id, EntityType.PROJECT_PAPER)
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

            isAllowedToReadProject(projectMemberRepo).checkFor(currentUser, projectPaper.projectId)

            val projectId = projectRepo.getProjectById(projectPaper.projectId).getOrThrow().id
            val adjacentPaper = repo.getAdjacentPaper(projectId, projectPaper.localPaperId, direction).getOrThrow()
            val paper = paperRepo.getPaperById(adjacentPaper.paperId).getOrThrow()

            adjacentPaper.toGrpcProjectPaperWithData(paper)
        }

    /**
     * Filters a list of [ProjectPaper]s paired with their associated reviews to retrieve those
     * that the current user has not reviewed.
     *
     * @param papersWithReviews A list of pairs, where each pair contains a [ProjectPaper] and a list of its [Review]s.
     * @param currentUser The [User] object representing the current user whose reviews are to be excluded.
     * @return A list of pairs, where each pair consists of a [ProjectPaper] and the count of its associated reviews.
     */
    private fun getProjectPapersWithoutOwnReview(
        papersWithReviews: List<Pair<ProjectPaper, List<Review>>>,
        currentUser: User,
    ) = papersWithReviews
        .filter { (_, reviews) ->
            reviews.none { review -> review.userId == currentUser.id }
        }
        .map { (paper, reviews) -> paper to reviews.size }

    /**
     * Sorts a list of project papers paired with their corresponding review counts. The sorting is done primarily by
     * the stage of the project paper in ascending order, and secondarily by the review count in ascending order. The
     * result includes only the sorted project papers, excluding the review counts from the output.
     *
     * @param projectPapersWithReviewCount A list of pairs, where each pair contains a [ProjectPaper] and its
     * corresponding review count, represented as an integer.
     * @return A list of [ProjectPaper] objects sorted by stage and review count.
     */
    private fun sortPapersByStageAndReviewCount(projectPapersWithReviewCount: List<Pair<ProjectPaper, Int>>) =
        projectPapersWithReviewCount
            .sortedWith(compareBy<Pair<ProjectPaper, Int>> { it.first.stage }.thenBy { it.second })
            .map { it.first }

    override suspend fun getProjectPaperById(request: Base.Id): GrpcProjectPaper = withUser(userRepo) { currentUser ->
        val projectPaperId = parseUUID(request.id, EntityType.PROJECT_PAPER)
        val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

        isAllowedToReadProject(projectMemberRepo).checkFor(currentUser, projectPaper.projectId)

        projectPaper.toGrpcProjectPaperWithData()
    }

    override suspend fun getProjectPaperByRelativeId(request: GrpcProjectPaper.Get): GrpcProjectPaper = withUser(
        userRepo,
    ) { currentUser ->
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)

        isAllowedToReadProject(projectMemberRepo)
            .andAlso(isProjectExistent(projectRepo))
            .checkFor(currentUser, projectId)

        val relativeId = request.relativeProjectPaperId.toLong()
        val projectPaper = repo.getProjectPaperByRelativeId(projectId, relativeId).getOrThrow()

        projectPaper.toGrpcProjectPaperWithData()
    }

    override suspend fun getAllProjectPapersForProject(request: Base.Id): GrpcProjectPaper.List =
        getProjectPapers(request)

    override suspend fun getPapersToReviewForProject(request: Base.Id): GrpcProjectPaper.List {
        val predicate: (ProjectPaperWithPaper, Map<ProjectPaper, List<GrpcReview>>, String) -> Boolean =
            { projectPaper, projectPaperReviewsMap, currentUserId ->
                val isAlreadyReviewedByCurrentUser = projectPaperReviewsMap[projectPaper.projectPaper]
                    ?.any { review -> review.userId == currentUserId } == true
                val isStillUndecided =
                    projectPaper.projectPaper.decision == PaperDecision.PAPER_DECISION_UNREVIEWED ||
                        projectPaper.projectPaper.decision == PaperDecision.PAPER_DECISION_IN_REVIEW

                !isAlreadyReviewedByCurrentUser && isStillUndecided
            }
        return getProjectPapers(request, predicate)
    }

    override suspend fun addPaperToProject(request: GrpcProjectPaper.Add): GrpcProjectPaper =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)
            val paperId = parseUUID(request.paperId, EntityType.PAPER)

            isServerOrProjectAdmin(projectMemberRepo, AccessType.CREATE).checkFor(currentUser, projectId)

            val project = projectRepo.getProjectById(projectId).getOrThrow()
            isProjectActive().checkFor(currentUser, project)

            val paper = paperRepo.getPaperById(paperId).getOrThrow()
            if (repo.doesProjectPaperExist(projectId, paperId)) {
                throw DuplicateEntityException(EntityType.PROJECT_PAPER, projectId.toString(), paperId.toString())
            }

            if (request.stage !in 0..project.maxStage) {
                throw OutOfRangeException.Stage(request.stage, project.maxStage)
            }

            val projectPaper = repo.addPaperToProject(request, currentUser.id)

            projectPaper.toGrpcProjectPaperWithData(paper)
        }

    override suspend fun getNextPaper(request: Base.Id): GrpcProjectPaper =
        getAdjacentPaper(request.id, PaperNavigationDirection.NEXT)

    override suspend fun getPreviousPaper(request: Base.Id): GrpcProjectPaper =
        getAdjacentPaper(request.id, PaperNavigationDirection.PREVIOUS)

    override suspend fun getNextPaperToReview(request: Base.Id): GrpcProjectPaper = withUser(userRepo) { currentUser ->
        val projectPaperId = parseUUID(request.id, EntityType.PROJECT_PAPER)
        val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()
        val projectId = projectPaper.projectId

        isAllowedToReadProject(projectMemberRepo).checkFor(currentUser, projectId)

        val projectPapers = repo.getLaterProjectPapers(
            projectId,
            projectPaper.localPaperId,
            projectPaper.stage,
        )
        val projectPapersWithReviews: List<Pair<ProjectPaper, List<Review>>> = projectPapers
            .map { paper ->
                val reviews = reviewTableRepo.getAllReviewsForProjectPaper(paper.id)
                paper to reviews
            }

        val projectPapersWithoutOwnReview = getProjectPapersWithoutOwnReview(projectPapersWithReviews, currentUser)

        if (projectPapersWithoutOwnReview.isEmpty()) {
            throw FailedPreconditionException(
                "There is no next project paper available to review in the project with ID $projectId",
            )
        }

        val sortedPapers = sortPapersByStageAndReviewCount(projectPapersWithoutOwnReview)

        val papersWithoutFinalDecision = sortedPapers.filter {
            it.decision == PaperDecision.PAPER_DECISION_UNSPECIFIED ||
                it.decision == PaperDecision.PAPER_DECISION_UNREVIEWED
        }

        (papersWithoutFinalDecision.firstOrNull() ?: sortedPapers.first()).toGrpcProjectPaperWithData()
    }
}
