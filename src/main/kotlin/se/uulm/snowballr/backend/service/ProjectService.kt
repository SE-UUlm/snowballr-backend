package se.uulm.snowballr.backend.service

import com.google.protobuf.timestamp
import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.dto.toGrpcProjectMembers
import se.uulm.snowballr.backend.model.dto.toGrpcProjects
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.accessrules.andAlso
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.forProperty
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import se.uulm.snowballr.backend.service.accessrules.isProjectExistent
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.isServerAdminOrSameUser
import se.uulm.snowballr.backend.service.accessrules.isServerOrProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
import snowballr.Base
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.copy
import java.util.UUID
import kotlin.getOrThrow
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion
import snowballr.ProjectOuterClass.Project as GrpcProject
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember

@Suppress("ComplexInterface")
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

    /**
     * Service implementation of [SnowballRService.getProjectInformation].
     */
    suspend fun getProjectInformation(request: GrpcProject.Information.Get): GrpcProject.Information

    /**
     * Service implementation of [SnowballRService.updateProjectMemberRole].
     */
    suspend fun updateProjectMemberRole(request: GrpcProjectMember.Update): Base.Nothing
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
 * @param projectPaperRepo The repository responsible for managing persistence operations for project papers.
 * @param criterionRepo The repository responsible for managing persistence operations for criteria.
 */
@Suppress("TooManyFunctions")
class ProjectService(
    private val repo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val criterionRepo: ICriterionTableRepo,
) : IProjectService {
    override suspend fun getProjectById(request: Base.Id): GrpcProject = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        isAllowedToReadProject(projectMemberRepo).checkFor(currentUser, projectId)

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
        projectMemberRepo.updateProjectMemberRole(project.id, currentUser.id, MemberRole.MEMBER_ROLE_ADMIN)

        project.toGrpcProject()
    }

    override suspend fun getAllProjects(): GrpcProject.List = withUser(userRepo) { currentUser ->
        isServerAdmin()
            .orElseThrow(UnauthorizedException.All(EntityType.PROJECT, AccessType.READ, currentUser.id.toString()))
            .checkFor(currentUser)

        repo.getAllProjects().toGrpcProjects()
    }

    private suspend fun getAllProjectsForUserAndStatus(
        request: Base.Id,
        statuses: Set<ProjectStatus>,
    ): GrpcProject.List = withUser(userRepo) { currentUser ->
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        val requestedUser = userRepo.getUserById(requestedUserId).getOrThrow()

        isServerAdminOrSameUser()
            .forProperty(User::id)
            .orElseThrow { requestingUser, _ ->
                UnauthorizedException.Single(
                    EntityType.USER,
                    requestedUserId.toString(),
                    AccessType.READ,
                    requestingUser.id.toString(),
                )
            }
            .checkFor(currentUser, requestedUser)

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

        isServerOrProjectAdmin(projectMemberRepo, AccessType.UPDATE).checkFor(currentUser, projectId)

        val project = repo.getProjectById(projectId).getOrThrow()
        val currentStatus = project.status

        val fieldMask = request.mask.pathsList
        val requestedStatus = request.project.status

        validateProjectUpdate(currentStatus, requestedStatus, fieldMask)

        val finalStatus = determineEffectiveProjectStatus(projectId, requestedStatus)
        val finalRequest = request.copy {
            this.project = request.project.copy {
                this.status = finalStatus
            }
        }

        repo.updateProject(finalRequest).toGrpcProject()
    }

    /**
     * Validates the update of a project based on the current status and the requested status.
     *
     * If the project is `PROJECT_STATUS_DELETED`, nothing can be updated.
     * If the project is `PROJECT_STATUS_ARCHIVED`, only the status can be updated (back to an active project).
     * If the project is `PROJECT_STATUS_ACTIVE`, then everything can be updated.
     * If the project is `PROJECT_STATUS_ACTIVE_LOCKED`, then everything except the slr settings can be updated.
     *
     * In addition, the status can never be set to `PROJECT_STATUS_DELETED` via the update method.
     *
     * @throws FailedPreconditionException if the update fails for any reason.
     */
    @Suppress("ThrowsCount")
    private fun validateProjectUpdate(
        currentStatus: ProjectStatus,
        requestedStatus: ProjectStatus,
        fieldMask: List<String>,
    ) {
        require(!(fieldMask.contains("project.status") && requestedStatus == ProjectStatus.PROJECT_STATUS_DELETED)) {
            "The project status cannot be set to DELETED via the update method. Use SoftDeleteProject instead."
        }

        when (currentStatus) {
            ProjectStatus.PROJECT_STATUS_DELETED -> {
                throw FailedPreconditionException(
                    "The project has been deleted and can therefore not be updated anymore.",
                )
            }

            ProjectStatus.PROJECT_STATUS_ARCHIVED -> {
                val isOnlyStatusUpdate = fieldMask.size == 1 && fieldMask.contains("project.status")
                if (!isOnlyStatusUpdate) {
                    throw FailedPreconditionException(
                        "The project is archived and therefore only the 'status' field can be updated.",
                    )
                }
                if (
                    requestedStatus != ProjectStatus.PROJECT_STATUS_ACTIVE &&
                    requestedStatus != ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED &&
                    requestedStatus != ProjectStatus.PROJECT_STATUS_ARCHIVED
                ) {
                    throw FailedPreconditionException(
                        "An archived project can only be unarchived by setting its status to ACTIVE or ACTIVE_LOCKED.",
                    )
                }
            }

            ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED -> {
                // all project settings are SLR settings
                val isChangingSettings = fieldMask.any { it.startsWith("project.settings.") }
                if (isChangingSettings) {
                    throw FailedPreconditionException(
                        "The project is locked and therefore no SLR settings can be modified.",
                    )
                }
            }

            ProjectStatus.PROJECT_STATUS_ACTIVE -> {}

            ProjectStatus.PROJECT_STATUS_UNSPECIFIED,
            ProjectStatus.UNRECOGNIZED,
            -> {
                error("Project is an unspecified status: $currentStatus")
            }
        }
    }

    /**
     * Determines the effective status of a project based on the current status and the requested status.
     *
     * If the requested status is ACTIVE or ACTIVE_LOCKED, it checks if the project has any papers with reviews. If so,
     * the project is ACTIVE_LOCKED; otherwise, it is set to ACTIVE.
     * If the requested status is not an active status, it is returned unchanged.
     *
     * @param projectId The ID of the project to be updated.
     * @param requestedStatus The status that is requested for the project.
     * @return The final status of the project.
     */
    private suspend fun determineEffectiveProjectStatus(
        projectId: UUID,
        requestedStatus: ProjectStatus,
    ): ProjectStatus {
        if (requestedStatus != ProjectStatus.PROJECT_STATUS_ACTIVE &&
            requestedStatus != ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
        ) {
            return requestedStatus
        }

        return if (repo.isProjectLocked(projectId)) {
            ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
        } else {
            ProjectStatus.PROJECT_STATUS_ACTIVE
        }
    }

    override suspend fun getProjectMembers(request: Base.Id): GrpcProjectMember.List =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.id, EntityType.PROJECT)

            isAllowedToReadProject(projectMemberRepo)
                .andAlso(isProjectExistent(repo))
                .checkFor(currentUser, projectId)

            val projectMembersWithUsers = projectMemberRepo.getProjectMembersWithUsers(projectId)
            projectMembersWithUsers.toGrpcProjectMembers()
        }

    override suspend fun getProjectInformation(request: GrpcProject.Information.Get): GrpcProject.Information =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)

            isProjectExistent(repo)
                .andAlso(isAllowedToReadProject(projectMemberRepo))
                .checkFor(currentUser, projectId)

            val project = repo.getProjectById(projectId).getOrThrow()
            val progress = projectPaperRepo.getProjectProgress(projectId)
            val builder = GrpcProject.Information.newBuilder()
            val has = if (request.hasMask() && request.mask.pathsList.isNotEmpty()) {
                val fieldMaskPaths = FieldMaskUtil.normalize(request.mask).pathsList.toSet();
                { path: String -> path in fieldMaskPaths }
            } else {
                { _ -> true }
            }

            if (has("project_progress")) {
                builder.setProjectProgress(progress)
            }

            if (has("creation_date")) {
                builder.setCreationDate(timestamp { seconds = project.createdAt.toEpochSecond() })
            }

            if (has("last_stage_started")) {
                builder.setLastStageStarted(timestamp { seconds = project.currentStageStartedAt.toEpochSecond() })
            }

            builder.build()
        }

    override suspend fun updateProjectMemberRole(request: GrpcProjectMember.Update): Base.Nothing {
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)
            val userId = parseUUID(request.userId, EntityType.USER)

            isServerOrProjectAdmin(projectMemberRepo, AccessType.UPDATE).checkFor(currentUser, projectId)

            repo.getProjectById(projectId).getOrThrow()
            userRepo.getUserById(userId).getOrThrow()

            val currentMember = try {
                projectMemberRepo.getProjectMemberByComposedId(projectId, userId).getOrThrow()
            } catch (_: NotFoundException) {
                throw FailedPreconditionException(
                    "User with ID '$userId' is not a member of project with ID '$projectId'.",
                )
            }

            if (currentMember.role == MemberRole.MEMBER_ROLE_ADMIN && request.newRole != MemberRole.MEMBER_ROLE_ADMIN) {
                val projectAdmins = projectMemberRepo.getAllProjectAdmins(projectId)
                if (projectAdmins.size <= 1) {
                    throw FailedPreconditionException("Cannot demote the last admin of a project.")
                }
            }

            projectMemberRepo.updateProjectMemberRole(projectId, userId, request.newRole)
        }

        return Base.Nothing.getDefaultInstance()
    }
}
