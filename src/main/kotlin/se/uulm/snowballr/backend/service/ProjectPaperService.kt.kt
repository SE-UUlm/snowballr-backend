package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPaper
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPapers
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import snowballr.Base
import snowballr.PaperOuterClass
import snowballr.ProjectOuterClass
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
 * @param authorOfPaperTableRepo The repository responsible for managing persistence operations for the author
 * paper relation.
 * @param citationTableRepo The repository responsible for managing persistence operations for the citation relation.
 * @param reviewTableRepo The repository responsible for managing persistence operations for the reviews
 * @param reviewHasCriterionTableRepo The repository responsible for managing persistence operations for the review has
 */
@Suppress("LongParameterList")
class ProjectPaperService(
    private val repo: IProjectPaperTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val authorOfPaperTableRepo: IAuthorOfPaperTableRepo,
    private val citationTableRepo: ICitationTableRepo,
    private val reviewTableRepo: IReviewTableRepo,
    private val reviewHasCriterionTableRepo: IReviewHasCriterionTableRepo,
) : IProjectPaperService {
    /**
     * Retrieves the gRPC representation of a project's paper, including associated data such as authors,
     * backward references, and reviews. Furthermore, it ensures that the current user has the necessary permissions to access
     * the project's paper.
     *
     * @param projectPaper The project paper, containing the ids linking to the associated data.
     * @param projectId The unique identifier of the project to which the paper belongs.
     * @return The gRPC representation of the project's paper, including associated data.
     * @throws UnauthorizedException.Single If the current user does not have the required read access to the project.
     */
    private suspend fun loadAndAuthorizeProjectPaper(projectPaper: ProjectPaper, projectId: UUID): GrpcProjectPaper {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectMembers = projectMemberRepo.getProjectMembers(projectId)

        if (projectMembers.none { it.userId == currentUser.id }) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }

        val paper = paperRepo.getPaperById(projectPaper.paperId)
        val authors = authorOfPaperTableRepo
            .getAuthorsOfPaperById(paper.id)
            .map { it.toGrpcAuthor() }

        val backwardReferences = citationTableRepo
            .getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            .map { it.toString() }

        val reviews = reviewTableRepo
            .getAllReviewsForProjectPaper(projectPaper.id)
            .map {
                val selectedCriteriaIds = reviewHasCriterionTableRepo.getSelectedCriteriaIdsForReviewById(it.id)
                it.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
            }

        return ProjectPaperWithPaper(projectPaper, paper).toGrpcProjectPaper(authors, backwardReferences, reviews)
    }

    /**
     * Retrieves a list of [ProjectPaper] associated with a specified project id. This method
     * also ensures access control for the current user. Optionally, a predicate function can be provided to filter
     * the [ProjectPaper]s based on custom criteria.
     *
     * @param request The request containing the ID of the [Project] for which [ProjectPaper]s are to be retrieved.
     * @param predicate An optional lambda function that takes a [ProjectPaperWithPaper], a map of [GrpcReview], and a user ID.
     * This function should return a boolean value to filter the [ProjectPaperWithPaper]s. If null, no filtering is applied.
     * @return A list of [GrpcProjectPaper] including associated metadata such as authors,
     * backward references, and reviews.
     * @throws UnauthorizedException If the user does not have the required access to the project.
     */
    private suspend fun getProjectPapers(
        request: Base.Id,
        predicate: (suspend (ProjectPaperWithPaper, Map<ProjectPaper, List<GrpcReview>>, String) -> Boolean)? = null,
    ): GrpcProjectPaper.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectId = parseUUID(request.id, EntityType.PROJECT)
        if (!projectRepo.doesProjectExistById(projectId)) {
            throw SnowballRException.NotFoundException(EntityType.PROJECT, projectId.toString())
        }

        if (!isProjectMember(projectMemberRepo, projectId, currentUser.id)) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }

        var projectPapersWithPapers = repo.getAllProjectPapersWithPapers(projectId)
        val paperAuthorsMap = mutableMapOf<Paper, List<PaperOuterClass.Author>>()
        val paperBackwardReferencesMap = mutableMapOf<Paper, List<String>>()
        val projectPaperReviewsMap = mutableMapOf<ProjectPaper, List<GrpcReview>>()
        for (projectPaper in projectPapersWithPapers) {
            val paper = projectPaper.paper
            paperAuthorsMap[paper] = authorOfPaperTableRepo
                .getAuthorsOfPaperById(paper.id).map { it.toGrpcAuthor() }
            paperBackwardReferencesMap[paper] = citationTableRepo
                .getBackwardsReferencedPaperIdsOfPaperById(paper.id).map {
                    it.toString()
                }
            projectPaperReviewsMap[projectPaper.projectPaper] = reviewTableRepo
                .getAllReviewsForProjectPaper(projectPaper.projectPaper.id)
                .map {
                    val selectedCriteriaIds = reviewHasCriterionTableRepo
                        .getSelectedCriteriaIdsForReviewById(it.id)
                    it.toGrpcReview(selectedCriteriaIds.map { criterion -> criterion.toString() })
                }
        }
        projectPapersWithPapers = predicate?.let { pred ->
            projectPapersWithPapers.filter { pred(it, projectPaperReviewsMap, currentUser.id.toString()) }
        } ?: projectPapersWithPapers

        return projectPapersWithPapers.toGrpcProjectPapers(
            paperAuthorsMap,
            paperBackwardReferencesMap,
            projectPaperReviewsMap,
        )
    }

    /**
     * Converts a combination of [Paper] and [ProjectPaper] into a gRPC-compatible [GrpcProjectPaper] object.
     * This includes enriching the data with associated authors, backward references, and reviews.
     *
     * @param paper The [Paper] containing base paper details.
     * @param projectPaper The [ProjectPaper] linking the [Paper] to a specific project and its metadata.
     * @return A [GrpcProjectPaper] object that encapsulates the paper, associated metadata, authors,
     * backward references, and reviews.
     */
    private suspend fun getGrpcProjectPaper(paper: Paper, projectPaper: ProjectPaper): GrpcProjectPaper {
        val authors = authorOfPaperTableRepo.getAuthorsOfPaperById(paper.id).map { it.toGrpcAuthor() }
        val backwardReferences = citationTableRepo.getBackwardsReferencedPaperIdsOfPaperById(
            paper.id,
        ).map { it.toString() }
        val reviews = reviewTableRepo.getAllReviewsForProjectPaper(projectPaper.id)
            .map {
                val selectedCriteriaIds = reviewHasCriterionTableRepo.getSelectedCriteriaIdsForReviewById(it.id)
                it.toGrpcReview(selectedCriteriaIds.map { criterion -> criterion.toString() })
            }
        return ProjectPaperWithPaper(projectPaper, paper).toGrpcProjectPaper(authors, backwardReferences, reviews)
    }

    override suspend fun getProjectPaperById(request: Base.Id): GrpcProjectPaper {
        val projectPaperId = parseUUID(request.id, EntityType.PROJECT_PAPER)
        val projectPaper = repo.getProjectPaperById(projectPaperId)
        return loadAndAuthorizeProjectPaper(projectPaper, projectPaper.projectId)
    }

    override suspend fun getProjectPaperByRelativeId(request: GrpcProjectPaper.Get): GrpcProjectPaper {
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)
        if (!projectRepo.doesProjectExistById(projectId)) {
            throw SnowballRException.NotFoundException(EntityType.PROJECT, projectId.toString())
        }

        val relativeId = request.relativeProjectPaperId.toLong()
        val projectPaper = repo.getProjectPaperByRelativeId(projectId, relativeId)
        return loadAndAuthorizeProjectPaper(projectPaper, projectId)
    }

    override suspend fun getAllProjectPapersForProject(request: Base.Id): GrpcProjectPaper.List =
        getProjectPapers(request)

    override suspend fun getPapersToReviewForProject(request: Base.Id): GrpcProjectPaper.List {
        val predicate: (ProjectPaperWithPaper, Map<ProjectPaper, List<GrpcReview>>, String) -> Boolean =
            { projectPaper, projectPaperReviewsMap, currentUserId ->
                val isAlreadyReviewedByCurrentUser = projectPaperReviewsMap[projectPaper.projectPaper]
                    ?.any { review -> review.userId == currentUserId } == true
                val isStillUndecided =
                    projectPaper.projectPaper.decision == ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED ||
                        projectPaper.projectPaper.decision ==
                        ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW

                !isAlreadyReviewedByCurrentUser && isStillUndecided
            }
        return getProjectPapers(request, predicate)
    }

    override suspend fun addPaperToProject(request: GrpcProjectPaper.Add): GrpcProjectPaper {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)

        if (!isProjectMember(projectMemberRepo, projectId, currentUser.id)) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }

        val paperId = parseUUID(request.paperId, EntityType.PAPER)
        val project = projectRepo.getProjectById(projectId)
        val paper = paperRepo.getPaperById(paperId)
        if (repo.doesProjectPaperExist(projectId, paperId)) {
            throw SnowballRException.DuplicateEntityException(
                EntityType.PROJECT_PAPER,
                projectId.toString(),
                paperId.toString(),
            )
        }

        if (request.stage !in 0..project.maxStage) {
            throw SnowballRException.OutOfRangeException.Stage(request.stage, project.maxStage)
        }

        val projectPaper = repo.addPaperToProject(request, currentUser.id)

        return getGrpcProjectPaper(paper, projectPaper)
    }
}
