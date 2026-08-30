package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.project.ProjectField
import se.uulm.snowballr.backend.model.dto.project.ProjectInfoField
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.notfound.StageNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.fetcher.FetcherOptions
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.outgoing.project.ProjectDecisionCount
import se.uulm.snowballr.backend.model.outgoing.project.ProjectDecisionStatistics
import se.uulm.snowballr.backend.model.outgoing.project.ProjectInformation
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Suppress("ComplexInterface")
interface IProjectService {
    /**
     * Service implementation of [SnowballRService.getProjectById].
     */
    suspend fun getProjectById(projectId: UUID): ProjectResponse

    /**
     * Service implementation of [SnowballRService.createProject].
     */
    suspend fun createProject(request: CreateProjectRequest): ProjectResponse

    /**
     * Service implementation of [SnowballRService.getAllProjects].
     */
    suspend fun getAllProjects(): List<ProjectResponse>

    /**
     * Service implementation of [SnowballRService.getAllProjectsForUser].
     */
    suspend fun getAllProjectsForUser(userId: UUID): List<ProjectResponse>

    /**
     * Service implementation of [SnowballRService.getAllArchivedProjectsForUser].
     */
    suspend fun getAllArchivedProjectsForUser(userId: UUID): List<ProjectResponse>

    /**
     * Service implementation of [SnowballRService.getAllDeletedProjectsForUser].
     */
    suspend fun getAllDeletedProjectsForUser(userId: UUID): List<ProjectResponse>

    /**
     * Service implementation of [SnowballRService.updateProject].
     */
    suspend fun updateProject(request: UpdateProjectRequest, fields: Set<ProjectField>): ProjectResponse

    /**
     * Service implementation of [SnowballRService.getProjectInformation].
     */
    suspend fun getProjectInformation(projectId: UUID, fields: Set<ProjectInfoField>): ProjectInformation

    /**
     * Service implementation of [SnowballRService.getDecisionStatisticsForStage].
     */
    suspend fun getDecisionStatisticsForStage(projectId: UUID, stage: Int): ProjectDecisionStatistics

    /**
     * Service implementation of [SnowballRService.softDeleteProject].
     */
    suspend fun softDeleteProject(projectId: UUID)
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
 * @param invitationTokenRepo The repository responsible for managing persistence operations for invitation tokens.
 * @param accessChecker Interface for checking access permissions for projects based on defined rules.
 * @param fetcherManager The [IFetcherManager] that manages the available fetchers.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class ProjectService(
    private val repo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val invitationTokenRepo: IInvitationTokenTableRepo,
    private val accessChecker: IProjectAccessChecker,
    private val fetcherManager: IFetcherManager,
) : IProjectService {
    override suspend fun getProjectById(projectId: UUID): ProjectResponse = withUser(userRepo) { currentUser ->
        accessChecker.isAllowedToReadProject(currentUser, projectId)

        val project = repo.getProjectById(projectId).getOrThrow()
        ProjectResponse.fromProject(project)
    }

    override suspend fun createProject(request: CreateProjectRequest): ProjectResponse =
        withUser(userRepo) { currentUser ->
            val userDefaultCriteria = criterionRepo.getCriteriaByIds(currentUser.settings.criteriaIds)

            val project = repo.createProject(request, currentUser.id, currentUser.settings.defaultProjectSettings)

            // Additionally, clone user default criteria into the project as project criteria and add creator as project member
            for (criterion in userDefaultCriteria) {
                val createCriterionRequest = CreateCriterionRequest(
                    tag = criterion.tag,
                    name = criterion.name,
                    description = criterion.description,
                    category = criterion.category,
                    projectId = project.id,
                )

                criterionRepo.createCriterion(createCriterionRequest, currentUser.id)
            }

            projectMemberRepo.addUserToProject(currentUser.id, project.id)
            projectMemberRepo.updateProjectMemberRole(project.id, currentUser.id, MemberRole.ADMIN)

            logger.info { "Project '${project.name}' (${project.id}) created" }
            ProjectResponse.fromProject(project)
        }

    override suspend fun getAllProjects(): List<ProjectResponse> = withUser(userRepo) { currentUser ->
        accessChecker.isAllowedToReadAllProjects(currentUser)

        repo.getAllProjects().map { ProjectResponse.fromProject(it) }
    }

    override suspend fun getAllProjectsForUser(userId: UUID): List<ProjectResponse> = getAllProjectsForUserAndStatus(
        userId,
        setOf(ProjectStatus.ACTIVE, ProjectStatus.ACTIVE_LOCKED),
    )

    override suspend fun getAllArchivedProjectsForUser(userId: UUID): List<ProjectResponse> =
        getAllProjectsForUserAndStatus(userId, setOf(ProjectStatus.ARCHIVED))

    override suspend fun getAllDeletedProjectsForUser(userId: UUID): List<ProjectResponse> =
        getAllProjectsForUserAndStatus(userId, setOf(ProjectStatus.DELETED))

    override suspend fun updateProject(request: UpdateProjectRequest, fields: Set<ProjectField>): ProjectResponse =
        withUser(userRepo) { currentUser ->
            accessChecker.isProjectOrServerAdmin(currentUser, request.projectId, AccessType.UPDATE)

            val project = repo.getProjectById(request.projectId).getOrThrow()
            val currentStatus = project.status

            validateProjectUpdate(currentStatus, request.status, fields)

            val finalStatus = determineEffectiveProjectStatus(request.projectId, request.status)
            var finalRequest = request.copy(status = finalStatus)

            if (fields.contains(ProjectField.FETCHERS)) {
                val sanitizedFetchersMap = sanitizeFetchersMap(finalRequest.settings.fetchers)

                finalRequest = finalRequest.copy(settings = finalRequest.settings.copy(fetchers = sanitizedFetchersMap))
            }

            val updatedProject = repo.updateProject(finalRequest, fields)
            if (fields.contains(ProjectField.STATUS) && currentStatus != finalStatus) {
                logger.info { "Project ${request.projectId} status changed: $currentStatus -> $finalStatus" }
            } else {
                logger.info { "Project ${request.projectId} updated: ${fields.joinToString()}" }
            }
            ProjectResponse.fromProject(updatedProject)
        }

    override suspend fun getProjectInformation(projectId: UUID, fields: Set<ProjectInfoField>): ProjectInformation =
        withUser(userRepo) { currentUser ->
            accessChecker.isAllowedToReadProject(currentUser, projectId)

            val project = repo.getProjectById(projectId).getOrThrow()
            var info = ProjectInformation(0F, OffsetDateTime.MIN, OffsetDateTime.MIN)

            val has = if (fields.isNotEmpty()) {
                { field: ProjectInfoField -> field in fields }
            } else {
                { _ -> true }
            }

            if (has(ProjectInfoField.PROJECT_PROGRESS)) {
                val progress = projectPaperRepo.getProjectProgress(projectId)
                info = info.copy(progress = progress)
            }

            if (has(ProjectInfoField.CREATION_DATE)) {
                info = info.copy(creationDate = project.createdAt)
            }

            if (has(ProjectInfoField.LAST_STAGE_STARTED)) {
                info = info.copy(lastStageStarted = project.currentStageStartedAt)
            }

            info
        }

    override suspend fun getDecisionStatisticsForStage(projectId: UUID, stage: Int): ProjectDecisionStatistics =
        withUser(userRepo) { currentUser ->
            accessChecker.isAllowedToReadProject(currentUser, projectId)

            val project = repo.getProjectById(projectId).getOrThrow()
            val maxStage = project.maxStage

            if (stage > maxStage) {
                throw StageNotFoundException(stage)
            }

            ProjectDecisionStatistics(statistics = createStatistics(projectId, stage))
        }

    override suspend fun softDeleteProject(projectId: UUID) = withUser(userRepo) { currentUser ->
        accessChecker.isProjectOrServerAdmin(currentUser, projectId, AccessType.DELETE)

        if (!repo.doesProjectExistById(projectId)) {
            throw ProjectNotFoundException(projectId)
        }

        repo.softDeleteProject(projectId)
        invitationTokenRepo.deleteInvitationTokensForProject(projectId)
        logger.info { "Project $projectId soft-deleted" }
    }

    private suspend fun getAllProjectsForUserAndStatus(
        userId: UUID,
        statuses: Set<ProjectStatus>,
    ): List<ProjectResponse> = withUser(userRepo) { currentUser ->
        userRepo.getUserById(userId).getOrThrow()

        accessChecker.isAllowedToReadUserProjects(currentUser, userId)

        repo.getUserProjects(userId, statuses).map { ProjectResponse.fromProject(it) }
    }

    /**
     * Validates the update of a project based on the current status and the requested status.
     *
     * If the project status is `DELETED`, nothing can be updated.
     * If the project status is `ARCHIVED`, only the status can be updated (back to an active project).
     * If the project status is `ACTIVE`, then everything can be updated.
     * If the project status is `ACTIVE_LOCKED`, then everything except the slr settings can be updated.
     *
     * In addition, the status can never be set to `DELETED` via the update method.
     *
     * @throws FailedPreconditionException if the update fails for any reason.
     */
    @Suppress("ThrowsCount")
    private fun validateProjectUpdate(
        currentStatus: ProjectStatus,
        requestedStatus: ProjectStatus,
        fields: Set<ProjectField>,
    ) {
        val isStatusUpdate = fields.contains(ProjectField.STATUS)
        require(!(isStatusUpdate && requestedStatus == ProjectStatus.DELETED)) {
            "The project status cannot be set to DELETED via the update method. Use SoftDeleteProject instead."
        }

        when (currentStatus) {
            ProjectStatus.DELETED ->
                throw FailedPreconditionException(
                    "The project has been deleted and can therefore not be updated anymore.",
                )

            ProjectStatus.ARCHIVED -> {
                val isOnlyStatusUpdate = fields.size == 1 && isStatusUpdate
                if (!isOnlyStatusUpdate) {
                    throw FailedPreconditionException(
                        "The project is archived and therefore only the 'status' field can be updated.",
                    )
                }

                if (
                    requestedStatus != ProjectStatus.ACTIVE &&
                    requestedStatus != ProjectStatus.ACTIVE_LOCKED &&
                    requestedStatus != ProjectStatus.ARCHIVED
                ) {
                    throw FailedPreconditionException(
                        "An archived project can only be unarchived by setting its status to ACTIVE or ACTIVE_LOCKED.",
                    )
                }
            }

            ProjectStatus.ACTIVE_LOCKED -> {
                // all project settings are SLR settings
                val isChangingSettings = fields.any { it.isSettingsField() }
                if (isChangingSettings) {
                    throw FailedPreconditionException(
                        "The project is locked and therefore no SLR settings can be modified.",
                    )
                }
            }
            ProjectStatus.ACTIVE -> { /* no restrictions */ }
            ProjectStatus.CLEARED -> error("Project is an unspecified status: $currentStatus")
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
        if (requestedStatus != ProjectStatus.ACTIVE && requestedStatus != ProjectStatus.ACTIVE_LOCKED) {
            return requestedStatus
        }

        return if (repo.isProjectLocked(projectId)) {
            ProjectStatus.ACTIVE_LOCKED
        } else {
            ProjectStatus.ACTIVE
        }
    }

    /**
     * Creates a list of [ProjectDecisionCount] objects for the given [stage] in the project with the ID [projectId].
     *
     * @param projectId The ID of the project for which the statistics should be created.
     * @param stage The stage for which the statistics should be created.
     * @return A list of [ProjectDecisionCount] objects.
     */
    private suspend fun createStatistics(projectId: UUID, stage: Int): List<ProjectDecisionCount> {
        val counts = projectPaperRepo.getAllProjectPapersForProject(projectId)
            .asSequence()
            .filter { it.stage == stage }
            .groupingBy { it.decision }
            .eachCount()
            .mapValues { it.value }

        fun createStatistic(decision: PaperDecision) = ProjectDecisionCount(decision, counts[decision] ?: 0)

        return listOf(
            PaperDecision.ACCEPTED,
            PaperDecision.DECLINED,
            PaperDecision.UNREVIEWED,
            PaperDecision.IN_REVIEW,
        ).map(::createStatistic)
    }

    /**
     * Sanitizes the passed [fetchers].
     *
     * This includes:
     * - excluding fetchers that are not registered in the application
     * - excluding fetcher options that are not registered for the specific fetcher
     *
     * This enables that no non-existent fetcher or non-existent fetcher option is stored in the database.
     */
    private suspend fun sanitizeFetchersMap(fetchers: FetcherMap): FetcherMap {
        val sanitizedFetchersMap = mutableMapOf<String, FetcherOptions>()
        val availableFetchers = fetcherManager.getAvailableFetchers()

        val fetcherIndex = availableFetchers.associateBy { it.id }
        for ((fetcher, options) in fetchers) {
            val info = fetcherIndex[fetcher]?.information ?: continue

            // Filter out non-existent options
            val availableOptions = info.optionsSchema
            val sanitizedOptions = options.filter { availableOptions.containsKey(it.key) }

            val requiredOptions = availableOptions.filter { it.value.isRequired }
            val missingRequiredOptions = requiredOptions.keys
                .filter { !sanitizedOptions.containsKey(it) || sanitizedOptions[it].isNullOrEmpty() }
            if (missingRequiredOptions.isNotEmpty()) {
                throw FailedPreconditionException(
                    "The following required options were not provided: $missingRequiredOptions",
                )
            }

            sanitizedFetchersMap[fetcher] = sanitizedOptions
        }

        return sanitizedFetchersMap
    }
}
