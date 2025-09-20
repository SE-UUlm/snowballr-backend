package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.dto.toGrpcProjectMembers
import se.uulm.snowballr.backend.model.dto.toGrpcProjects
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.Base
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion
import snowballr.ProjectOuterClass.Project as GrpcProject
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember

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
     */
    suspend fun updateProject(request: GrpcProject.Update): GrpcProject

    /**
     * Service implementation of [SnowballRService.getProjectMembers].
     */
    suspend fun getProjectMembers(request: Base.Id): GrpcProjectMember.List
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
 */
class ProjectService(
    private val repo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val criterionRepo: ICriterionTableRepo,
) : IProjectService {
    override suspend fun getProjectById(request: Base.Id): GrpcProject = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        ensureCurrentUserIsProjectMember(projectMemberRepo, projectId, currentUser)

        repo.getProjectById(projectId).getOrThrow().toGrpcProject()
    }

    override suspend fun createProject(request: GrpcProject.Create): GrpcProject = withUser(userRepo) { currentUser ->
        val userSettings = userRepo.getUserSettings(currentUser.id).getOrThrow()
        val userDefaultCriteria = criterionRepo.getCriteriaByIds(userSettings.criteriaIds)

        val project = repo.createProject(request, currentUser.id, userSettings)

        // Additionally, clone user default criteria into the project as project criteria and add creator as project member
        for (criterion in userDefaultCriteria) {
            val criterionRequest = GrpcCriterion.Create
                .newBuilder()
                .setTag(criterion.tag)
                .setName(criterion.name)
                .setDescription(criterion.description)
                .setCategory(criterion.category)
                .setProjectId(project.id.toString())
                .build()

            criterionRepo.createCriterion(criterionRequest, currentUser.id)
        }

        projectMemberRepo.addUserToProject(currentUser.id, project.id)
        projectMemberRepo.promoteProjectMemberToAdmin(project.id, currentUser.id)

        project.toGrpcProject()
    }

    override suspend fun getAllProjects(): GrpcProject.List = withUser(userRepo) { currentUser ->
        verifyServerAdminRole(currentUser) { UnauthorizedException.All(EntityType.PROJECT, AccessType.READ, it) }

        repo.getAllProjects().toGrpcProjects()
    }

    private suspend fun getAllProjectsForUserAndStatus(
        request: Base.Id,
        statuses: Set<ProjectStatus>,
    ): GrpcProject.List = withUser(userRepo) { currentUser ->
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(currentUser, requestedUserId, userRepo, AccessType.READ)

        repo.getUserProjects(requestedUserId, statuses).toGrpcProjects()
    }

    override suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List = getAllProjectsForUserAndStatus(
        request,
        setOf(ProjectStatus.PROJECT_STATUS_ACTIVE, ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED),
    )

    override suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List =
        getAllProjectsForUserAndStatus(request, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))

    override suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List =
        getAllProjectsForUserAndStatus(request, setOf(ProjectStatus.PROJECT_STATUS_DELETED))

    override suspend fun updateProject(request: GrpcProject.Update): GrpcProject = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.project.id, EntityType.PROJECT)
        val project = repo.getProjectById(projectId).getOrThrow()
        val isProjectAdmin = projectMemberRepo.getAllProjectAdmins(projectId).any { it.userId == currentUser.id }
        val projectStatus = project.status

        if (!isProjectAdmin) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(EntityType.PROJECT, request.project.id, AccessType.UPDATE, it)
            }
        }
        if (project.status == ProjectStatus.PROJECT_STATUS_DELETED) {
            throw FailedPreconditionException("The project with the id ${request.project.id} is deleted.")
        }
        if (request.project.status == ProjectStatus.PROJECT_STATUS_DELETED) {
            throw FailedPreconditionException("The project status can not be set to deleted by an update call.")
        }

        repo.updateProject(request, projectStatus).toGrpcProject()
    }

    override suspend fun getProjectMembers(request: Base.Id): GrpcProjectMember.List =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.id, EntityType.PROJECT)
            repo.getProjectById(projectId).getOrThrow()
            val projectMembersWithUsers = projectMemberRepo.getProjectMembersWithUsers(projectId)

            if (!projectMembersWithUsers.any { it.user.id == currentUser.id }) {
                verifyServerAdminRole(currentUser) {
                    throw UnauthorizedException.Single(EntityType.PROJECT, projectId.toString(), AccessType.READ, it)
                }
            }

            projectMembersWithUsers.toGrpcProjectMembers()
        }
}
