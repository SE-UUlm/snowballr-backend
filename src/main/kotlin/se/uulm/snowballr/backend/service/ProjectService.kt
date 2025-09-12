package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.dto.toGrpcProjectMembers
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPaper
import se.uulm.snowballr.backend.model.dto.toGrpcProjectPapers
import se.uulm.snowballr.backend.model.dto.toGrpcProjects
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
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
import snowballr.CriterionOuterClass
import snowballr.PaperOuterClass
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.ProjectStatus
import java.util.UUID
import snowballr.ProjectOuterClass.Project as GrpcProject
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper
import snowballr.ReviewOuterClass.Review as GrpcReview

@Suppress("ComplexInterface", "TooManyFunctions")
interface IProjectService {
    /**
     * Service implementation of [SnowballRService.getProjectById].
     */
    suspend fun getProjectById(request: Base.Id): GrpcProject

    /**
     * Service implementation of [SnowballRService.createProject].
     */
    suspend fun createProject(request: GrpcProject.Create): GrpcProject

    /**
     * Service implementation of [SnowballRService.getAllProjects].
     */
    suspend fun getAllProjects(): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllProjectsForUser].
     */
    suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllArchivedProjectsForUser].
     */
    suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllDeletedProjectsForUser].
     */
    suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.updateProject].
     *
     * @param request The update request containing the project details to be modified.
     * @return The updated project after the changes have been applied.
     */
    suspend fun updateProject(request: GrpcProject.Update): GrpcProject

    /**
     * Service implementation of [SnowballRService.getProjectMembers].
     */
    suspend fun getProjectMembers(request: Base.Id): GrpcProject.Member.List

    /**
     * Service implementation of [SnowballRService.getProjectPaperById].
     */
    suspend fun getProjectPaperById(request: Base.Id): GrpcProjectPaper

    /**
     * Service implementation of [SnowballRService.getProjectPaperByRelativeId].
     */
    suspend fun getProjectPaperByRelativeId(request: GrpcProject.Paper.Get): GrpcProject.Paper

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
 * The [ProjectService] class handles operations related to projects by implementing the [IProjectService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of project CRUD operations,
 * delegating the actual persistence operations to the [IProjectTableRepo] repository.
 *
 * @constructor Initializes the [ProjectService] with a project repository.
 * @param repo The repository responsible for managing persistence operations for projects.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 * @param criterionRepo The repository responsible for managing persistence operations for criteria.
 * @param paperRepo The repository responsible for managing persistence operations for papers.
 * @param projectPaperRepo The repository responsible for managing persistence operations for project papers.
 * @param authorOfPaperTableRepo The repository responsible for managing persistence operations for the author
 * paper relation.
 * @param citationTableRepo The repository responsible for managing persistence operations for the citation relation.
 * @param reviewTableRepo The repository responsible for managing persistence operations for the reviews
 * @param reviewHasCriterionTableRepo The repository responsible for managing persistence operations for the review has
 * criterion relation.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class ProjectService(
    private val repo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val authorOfPaperTableRepo: IAuthorOfPaperTableRepo,
    private val citationTableRepo: ICitationTableRepo,
    private val reviewTableRepo: IReviewTableRepo,
    private val reviewHasCriterionTableRepo: IReviewHasCriterionTableRepo,
) : IProjectService {
    /**
     * Checks whether a user is a member of a specific project.
     *
     * @param projectId The unique identifier of the project.
     * @param currentUserId The unique identifier of the user to be checked for membership.
     * @return `true` if the user is a member of the project, `false` otherwise.
     */
    private suspend fun isProjectMember(projectId: UUID, currentUserId: UUID): Boolean {
        val projectMembers = projectMemberRepo.getProjectMembers(projectId)
        return projectMembers.any { it.userId == currentUserId }
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
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val projectId = parseUUID(request.id, EntityType.PROJECT)
        if (!repo.doesProjectExistById(projectId)) {
            throw SnowballRException.NotFoundException(EntityType.PROJECT, projectId.toString())
        }

        if (!isProjectMember(projectId, currentUser.id)) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }

        var projectPapersWithPapers = projectPaperRepo.getAllProjectPapersWithPapers(projectId)
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
     * Retrieves the gRPC representation of a project's paper, including associated data such as authors,
     * backward references, and reviews. Furthermore, it ensures that the current user has the necessary permissions to access
     * the project's paper.
     *
     * @param projectPaper The project paper, containing the ids linking to the associated data.
     * @param projectId The unique identifier of the project to which the paper belongs.
     * @return The gRPC representation of the project's paper, including associated data.
     * @throws UnauthorizedException.Single If the current user does not have the required read access to the project.
     */
    private suspend fun loadAndAuthorizeProjectPaper(projectPaper: ProjectPaper, projectId: UUID): GrpcProject.Paper {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
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

        val paper = paperRepo.getPaperById(projectPaper.paperId).getOrThrow()
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

    override suspend fun getProjectById(request: Base.Id): GrpcProject {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        if (!isProjectMember(projectId, currentUser.id)) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }
        return repo.getProjectById(projectId).getOrThrow().toGrpcProject()
    }

    override suspend fun createProject(request: GrpcProject.Create): GrpcProject {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val userSettings = userRepo.getUserSettings(currentUser.id).getOrThrow()
        val userDefaultCriteria = criterionRepo.getCriteriaByIds(userSettings.criteriaIds)

        val project = repo.createProject(request, GrpcContext.getUserIdFromContext(), userSettings)

        for (criterion in userDefaultCriteria) {
            val criterionRequest = CriterionOuterClass.Criterion.Create
                .newBuilder()
                .setTag(criterion.tag)
                .setName(criterion.name)
                .setDescription(criterion.description)
                .setCategory(criterion.category)
                .setProjectId(project.id.toString())
                .build()

            criterionRepo.createCriterion(criterionRequest, currentUser.id)
        }
        return project.toGrpcProject()
    }

    override suspend fun getAllProjects(): GrpcProject.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()

        verifyServerAdminRole(currentUser) { UnauthorizedException.All(EntityType.PROJECT, AccessType.READ, it) }

        val projects = repo.getAllProjects()
        return projects.toGrpcProjects()
    }

    override suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val userProjects = repo.getUserProjects(requestedUserId)
        return userProjects.toGrpcProjects()
    }

    override suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val archivedUserProjects = repo.getUserProjects(requestedUserId, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))
        return archivedUserProjects.toGrpcProjects()
    }

    override suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val deletedUserProjects = repo.getUserProjects(requestedUserId, setOf(ProjectStatus.PROJECT_STATUS_DELETED))
        return deletedUserProjects.toGrpcProjects()
    }

    override suspend fun updateProject(request: GrpcProject.Update): GrpcProject {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val projectId = parseUUID(request.project.id, EntityType.PROJECT)
        val project = repo.getProjectById(projectId).getOrThrow()
        val isProjectAdmin = projectMemberRepo.getAllProjectAdmins(projectId).any { it.userId == currentUser.id }
        val projectStatus = project.status

        if (!isProjectAdmin) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    request.project.id,
                    AccessType.UPDATE,
                    it,
                )
            }
        }
        if (project.status == ProjectStatus.PROJECT_STATUS_DELETED) {
            throw SnowballRException.FailedPreconditionException(
                "The project with the id ${request.project.id} is deleted.",
            )
        }
        if (request.project.status == ProjectStatus.PROJECT_STATUS_DELETED) {
            throw SnowballRException.FailedPreconditionException(
                "The project status can not be set to deleted by an update call.",
            )
        }

        return repo.updateProject(request, projectStatus).toGrpcProject()
    }

    override suspend fun getProjectMembers(request: Base.Id): GrpcProject.Member.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val projectId = parseUUID(request.id, EntityType.PROJECT)
        repo.getProjectById(projectId)
        val projectMembersWithUsers = projectMemberRepo.getProjectMembersWithUsers(projectId)

        if (!projectMembersWithUsers.any { it.user.id == currentUser.id }) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }
        return projectMembersWithUsers.toGrpcProjectMembers()
    }

    override suspend fun getProjectPaperById(request: Base.Id): GrpcProject.Paper {
        val projectPaperId = parseUUID(request.id, EntityType.PROJECT_PAPER)
        val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()
        return loadAndAuthorizeProjectPaper(projectPaper, projectPaper.projectId)
    }

    override suspend fun getProjectPaperByRelativeId(request: GrpcProjectPaper.Get): GrpcProjectPaper {
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)
        if (!repo.doesProjectExistById(projectId)) {
            throw SnowballRException.NotFoundException(EntityType.PROJECT, projectId.toString())
        }

        val relativeId = request.relativeProjectPaperId.toLong()
        val projectPaper = projectPaperRepo.getProjectPaperByRelativeId(projectId, relativeId)
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
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)

        if (!isProjectMember(projectId, currentUser.id)) {
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
        val project = repo.getProjectById(projectId).getOrThrow()
        val paper = paperRepo.getPaperById(paperId).getOrThrow()
        if (projectPaperRepo.doesProjectPaperExist(projectId, paperId)) {
            throw SnowballRException.DuplicateEntityException(
                EntityType.PROJECT_PAPER,
                projectId.toString(),
                paperId.toString(),
            )
        }

        if (request.stage !in 0..project.maxStage) {
            throw SnowballRException.OutOfRangeException.Stage(request.stage, project.maxStage)
        }

        val projectPaper = projectPaperRepo.addPaperToProject(request, currentUser.id)

        return getGrpcProjectPaper(paper, projectPaper)
    }
}
