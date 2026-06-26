package se.uulm.snowballr.backend.service

import com.google.protobuf.timestamp
import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.dto.toGrpcProjects
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.notfound.StageNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.copy
import java.util.UUID
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion
import snowballr.Fetcher.FetcherOptions as GrpcFetcherOptions
import snowballr.ProjectOuterClass.Project as GrpcProject
import snowballr.ProjectOuterClass.Project.Information.DecisionStatistics as GrpcProjectDecisionStatistics

@Suppress("ComplexInterface")
interface IProjectService {
    /**
     * Service implementation of [SnowballRService.getProjectById].
     */
    suspend fun getProjectById(projectId: UUID): GrpcProject

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
    suspend fun getAllProjectsForUser(userId: UUID): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllArchivedProjectsForUser].
     */
    suspend fun getAllArchivedProjectsForUser(userId: UUID): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllDeletedProjectsForUser].
     */
    suspend fun getAllDeletedProjectsForUser(userId: UUID): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.updateProject].
     */
    suspend fun updateProject(request: GrpcProject.Update): GrpcProject

    /**
     * Service implementation of [SnowballRService.getProjectInformation].
     */
    suspend fun getProjectInformation(request: GrpcProject.Information.Get): GrpcProject.Information

    /**
     * Service implementation of [SnowballRService.getDecisionStatisticsForStage].
     */
    suspend fun getDecisionStatisticsForStage(request: GrpcProjectDecisionStatistics.Get): GrpcProjectDecisionStatistics

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
    typealias GrpcFetcherMap = Map<String, GrpcFetcherOptions>

    override suspend fun getProjectById(projectId: UUID): GrpcProject = withUser(userRepo) { currentUser ->
        accessChecker.isAllowedToReadProject(currentUser, projectId)

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
                .setCategory(criterion.category.toGrpc())
                .setProjectId(project.id.toString())
                .build()

            criterionRepo.createCriterion(criterionRequest, currentUser.id)
        }

        projectMemberRepo.addUserToProject(currentUser.id, project.id)
        projectMemberRepo.updateProjectMemberRole(project.id, currentUser.id, MemberRole.MEMBER_ROLE_ADMIN)

        project.toGrpcProject()
    }

    override suspend fun getAllProjects(): GrpcProject.List = withUser(userRepo) { currentUser ->
        accessChecker.isAllowedToReadAllProjects(currentUser)

        repo.getAllProjects().toGrpcProjects()
    }

    override suspend fun getAllProjectsForUser(userId: UUID): GrpcProject.List = getAllProjectsForUserAndStatus(
        userId,
        setOf(ProjectStatus.PROJECT_STATUS_ACTIVE, ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED),
    )

    override suspend fun getAllArchivedProjectsForUser(userId: UUID): GrpcProject.List =
        getAllProjectsForUserAndStatus(userId, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))

    override suspend fun getAllDeletedProjectsForUser(userId: UUID): GrpcProject.List =
        getAllProjectsForUserAndStatus(userId, setOf(ProjectStatus.PROJECT_STATUS_DELETED))

    override suspend fun updateProject(request: GrpcProject.Update): GrpcProject = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.project.id, EntityType.PROJECT)

        accessChecker.isProjectOrServerAdmin(currentUser, projectId, AccessType.UPDATE)

        val project = repo.getProjectById(projectId).getOrThrow()
        val currentStatus = project.status

        val fieldMask = FieldMaskUtil.normalize(request.mask).pathsList
        val requestedStatus = request.project.status

        validateProjectUpdate(currentStatus, requestedStatus, fieldMask)

        val finalStatus = determineEffectiveProjectStatus(projectId, requestedStatus)
        var finalRequest = request.copy {
            this.project = request.project.copy {
                this.status = finalStatus
            }
        }

        if (fieldMask.contains("project.settings.fetchers")) {
            val sanitizedFetchersMap = sanitizeFetchersMap(finalRequest.project.settings.fetchersMap)

            finalRequest = finalRequest.copy {
                this.project = this.project.copy {
                    this.settings = this.settings.toBuilder()
                        .clearFetchers()
                        .putAllFetchers(sanitizedFetchersMap)
                        .build()
                }
            }
        }

        repo.updateProject(finalRequest).toGrpcProject()
    }

    override suspend fun getProjectInformation(request: GrpcProject.Information.Get): GrpcProject.Information =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)

            accessChecker.isAllowedToReadProject(currentUser, projectId)

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

    override suspend fun getDecisionStatisticsForStage(
        request: GrpcProjectDecisionStatistics.Get,
    ): GrpcProjectDecisionStatistics = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)

        accessChecker.isAllowedToReadProject(currentUser, projectId)

        val project = repo.getProjectById(projectId).getOrThrow()
        val maxStage = project.maxStage

        if (request.stage > maxStage) {
            throw StageNotFoundException(request.stage)
        }

        val statistics = createStatistics(projectId, request.stage)
        GrpcProjectDecisionStatistics
            .newBuilder()
            .addAllStatistics(statistics)
            .build()
    }

    override suspend fun softDeleteProject(projectId: UUID) = withUser(userRepo) { currentUser ->
        accessChecker.isProjectOrServerAdmin(currentUser, projectId, AccessType.DELETE)

        if (!repo.doesProjectExistById(projectId)) {
            throw ProjectNotFoundException(projectId)
        }

        repo.softDeleteProject(projectId)
        invitationTokenRepo.deleteInvitationTokensForProject(projectId)
    }

    private suspend fun getAllProjectsForUserAndStatus(userId: UUID, statuses: Set<ProjectStatus>): GrpcProject.List =
        withUser(userRepo) { currentUser ->
            userRepo.getUserById(userId).getOrThrow()

            accessChecker.isAllowedToReadUserProjects(currentUser, userId)

            repo.getUserProjects(userId, statuses).toGrpcProjects()
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
        requestedStatus: ProjectOuterClass.ProjectStatus,
        fieldMask: List<String>,
    ) {
        val isStatusUpdate = fieldMask.contains("project.status")
        require(!(isStatusUpdate && requestedStatus == ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED)) {
            "The project status cannot be set to DELETED via the update method. Use SoftDeleteProject instead."
        }

        when (currentStatus) {
            ProjectStatus.PROJECT_STATUS_DELETED -> {
                throw FailedPreconditionException(
                    "The project has been deleted and can therefore not be updated anymore.",
                )
            }

            ProjectStatus.PROJECT_STATUS_ARCHIVED -> {
                val isOnlyStatusUpdate = fieldMask.size == 1 && isStatusUpdate
                if (!isOnlyStatusUpdate) {
                    throw FailedPreconditionException(
                        "The project is archived and therefore only the 'status' field can be updated.",
                    )
                }

                if (
                    requestedStatus != ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE &&
                    requestedStatus != ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED &&
                    requestedStatus != ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED
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

            ProjectStatus.PROJECT_STATUS_ACTIVE -> {
                // no restrictions
            }

            ProjectStatus.PROJECT_STATUS_CLEARED,
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
        requestedStatus: ProjectOuterClass.ProjectStatus,
    ): ProjectOuterClass.ProjectStatus {
        if (requestedStatus != ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE &&
            requestedStatus != ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
        ) {
            return requestedStatus
        }

        return if (repo.isProjectLocked(projectId)) {
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
        } else {
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
        }
    }

    /**
     * Creates a list of [GrpcProjectDecisionStatistics.Statistic] objects for the given [stage]
     * in the project with the ID [projectId].
     *
     * @param projectId The ID of the project for which the statistics should be created.
     * @param stage The stage for which the statistics should be created.
     * @return A list of [GrpcProjectDecisionStatistics.Statistic] objects.
     */
    private suspend fun createStatistics(projectId: UUID, stage: Long): List<GrpcProjectDecisionStatistics.Statistic> {
        val counts = projectPaperRepo.getAllProjectPapersForProject(projectId)
            .asSequence()
            .filter { it.stage == stage }
            .groupingBy { it.decision }
            .eachCount()
            .mapValues { it.value.toLong() }

        fun createStatistic(decision: PaperDecision): GrpcProjectDecisionStatistics.Statistic =
            GrpcProjectDecisionStatistics.Statistic.newBuilder()
                .setDecision(decision)
                .setCount(counts[decision] ?: 0)
                .build()

        return listOf(
            PaperDecision.PAPER_DECISION_ACCEPTED,
            PaperDecision.PAPER_DECISION_DECLINED,
            PaperDecision.PAPER_DECISION_UNREVIEWED,
            PaperDecision.PAPER_DECISION_IN_REVIEW,
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
    private suspend fun sanitizeFetchersMap(fetchers: GrpcFetcherMap): GrpcFetcherMap {
        val sanitizedFetchersMap = mutableMapOf<String, GrpcFetcherOptions>()
        val availableFetchers = fetcherManager.getAvailableFetchers()

        val fetcherIndex = availableFetchers.associateBy { it.id }
        for ((fetcher, options) in fetchers) {
            val info = fetcherIndex[fetcher] ?: continue

            // Filter out non-existent options
            val availableOptions = info.optionsSchemaMap
            val sanitizedOptions = options.optionsMap.filter { availableOptions.containsKey(it.key) }

            val requiredOptions = availableOptions.filter { it.value.required }
            val missingRequiredOptions = requiredOptions.keys
                .filter { !sanitizedOptions.containsKey(it) || sanitizedOptions[it].isNullOrEmpty() }
            if (missingRequiredOptions.isNotEmpty()) {
                throw FailedPreconditionException(
                    "The following required options were not provided: $missingRequiredOptions",
                )
            }

            sanitizedFetchersMap[fetcher] = GrpcFetcherOptions.newBuilder()
                .putAllOptions(sanitizedOptions)
                .build()
        }

        return sanitizedFetchersMap
    }
}
