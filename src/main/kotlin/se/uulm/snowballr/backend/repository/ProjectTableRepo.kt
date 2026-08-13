package se.uulm.snowballr.backend.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaper
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.user.UserSettings
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.ProjectField
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectSettingRequest
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.toProject
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger { }

/**
 * Defines an interface for repository operations related to the [ProjectTable].
 *
 * This interface is used to handle persistence and retrieval operations for projects by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing projects can remain decoupled from the specifics of the database layer.
 */
@Suppress("ComplexInterface")
interface IProjectTableRepo {
    /**
     * Returns a [Result] containing the project by its ID or a [NotFoundException] if the project with the passed [id]
     * doesn't exist.
     */
    suspend fun getProjectById(id: UUID): Result<Project>

    /**
     * Checks whether the project with the passed [id] exists.
     */
    suspend fun doesProjectExistById(id: UUID): Boolean

    /**
     * Creates a new project in the database with the provided project creation request and user ID.
     *
     * @param request The project creation request containing project details
     * @param userId The ID of the user creating the project.
     * @param userSettings The user's current settings, such as default criteria IDs, similarity threshold, and other
     * relevant preferences.
     * @return The created [Project] object representing the newly created project.
     */
    suspend fun createProject(request: CreateProjectRequest, userId: UUID, userSettings: UserSettings): Project

    /**
     * Returns all active projects stored in the database.
     */
    suspend fun getAllProjects(): List<Project>

    /**
     * Retrieves a list of projects from the database in which the user with the specified [userId] is a member.
     *
     * These projects can be filtered by their status, e.g., this function can be used to retrieve all archived
     * and deleted projects of a user.
     *
     * @param userId The unique identifier of the user whose associated projects are to be fetched.
     * @param statusFilters (optional) Set of filters to specify which project status the fetched projects should have.
     * By default, [ProjectStatus.ACTIVE] and [ProjectStatus.ACTIVE_LOCKED] are used, which includes both active and
     * active-locked projects, i.e., projects where settings cannot be changed anymore.
     * If set to another value (e.g., [ProjectStatus.DELETED]), only projects matching one of the statuses
     * (e.g., deleted) will be returned.
     *
     * @return A list of [Project] objects matching the specified filters where the given user is member of.
     */
    suspend fun getUserProjects(
        userId: UUID,
        statusFilters: Set<ProjectStatus> = setOf(
            ProjectStatus.ACTIVE,
            ProjectStatus.ACTIVE_LOCKED,
        ),
    ): List<Project>

    /**
     * Updates an existent project in the database with the provided new information.
     *
     * @param request The update request containing the new project details.
     * @param fields The fields that should be updated.
     * @return The updated [Project] object reflecting the changes from the [request].
     */
    suspend fun updateProject(request: UpdateProjectRequest, fields: Set<ProjectField>): Project

    /**
     * Checks if the project with the given [projectId] is locked.
     *
     * A project is considered **locked** if at least one [ProjectPaper] with at least one [Review]
     * is associated with this project.
     *
     * **Note:** This functions returns `true` or `false` regardless of the project's current state.
     * However, the result is only meaningful if the project is in an active state (i.e., [ProjectStatus.ACTIVE] or
     * [ProjectStatus.ACTIVE_LOCKED]).
     *
     * @param projectId The ID of the project to check for locked status.
     * @return `true` if the project is locked, `false` otherwise.
     */
    suspend fun isProjectLocked(projectId: UUID): Boolean

    /**
     * Performs a soft-delete of the project with the given [projectId], i.e., does not remove the
     * project from the database but marks it as deleted by setting the status to [ProjectStatus.DELETED].
     */
    suspend fun softDeleteProject(projectId: UUID)

    /**
     * Clears all soft-deleted projects whose deletion date is older than the given [thresholdDate].
     *
     * @param thresholdDate The date up to which soft-deleted projects are to be cleared.
     */
    suspend fun clearSoftDeletedProjects(thresholdDate: OffsetDateTime)

    /**
     * Tries to hard-delete all projects that were soft-deleted, cleared, and are no longer referenced by any other
     * entity.
     */
    suspend fun hardDeleteClearedProjects()

    /**
     * Ensures that the max stage of the project is greater than or equal to the passed [stage].
     *
     * If the max stage of the project is lower than [stage] then the project value is updated; otherwise nothing
     * happens.
     */
    suspend fun updateMaxStageIfExceeded(projectId: UUID, stage: Int)
}

/**
 * Repository implementation for managing the [ProjectTable] in the database.
 *
 * This class handles the persistence and retrieval of project data by integrating
 * with the underlying database through the [IDatabase] interface. It provides
 * concrete methods for CRUD operations on project records within the database.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
@Suppress("TooManyFunctions")
class ProjectTableRepo(
    private val db: IDatabase,
) : IProjectTableRepo {
    private fun getProjectByIdOrNull(id: UUID): Project? = ProjectTable.getEntityByIdOrNull(id, ResultRow::toProject)

    override suspend fun getProjectById(id: UUID): Result<Project> = db.query {
        getEntityByKeyAsResult(::getProjectByIdOrNull, EntityType.PROJECT, id)
    }

    override suspend fun doesProjectExistById(id: UUID): Boolean = db.query {
        ProjectTable.doesEntityExistById(id)
    }

    override suspend fun createProject(
        request: CreateProjectRequest,
        userId: UUID,
        userSettings: UserSettings,
    ): Project = db.query {
        ProjectTable.insertAndGet(ResultRow::toProject) {
            it[name] = request.name
            it[status] = ProjectStatus.ACTIVE
            it[currentStage] = 0
            it[maxStage] = 0
            it[similarityThreshold] = userSettings.similarityThreshold
            it[snowballingType] = userSettings.snowballingType
            it[reviewMaybeAllowed] = userSettings.reviewMaybeAllowed
            it[reviewDecisionMatrixBinary] = userSettings.decisionMatrix.toByteArray()
            it[fetchers] = emptyMap()
            it[createdBy] = userId
        }
    }

    override suspend fun getAllProjects(): List<Project> = db.query {
        ProjectTable.getEntities(ResultRow::toProject) {
            (ProjectTable.status eq ProjectStatus.ACTIVE) or (ProjectTable.status eq ProjectStatus.ACTIVE_LOCKED)
        }
    }

    override suspend fun getUserProjects(userId: UUID, statusFilters: Set<ProjectStatus>): List<Project> = db.query {
        val projectFilter = statusFilters
            .map { ProjectTable.status eq it }
            .reduceOrNull { acc, filter -> acc or filter }

        requireNotNull(projectFilter) {
            "Unsupported filter statuses: ${statusFilters.joinToString(", ") { it.name }}."
        }

        (ProjectTable innerJoin ProjectMemberTable)
            .select(ProjectTable.columns)
            .where { ProjectMemberTable.userId eq userId }
            .andWhere { projectFilter }
            .map { it.toProject() }
    }

    @Suppress("CognitiveComplexMethod")
    override suspend fun updateProject(request: UpdateProjectRequest, fields: Set<ProjectField>): Project = db.query {
        if (fields.isEmpty()) {
            return@query getProjectById(request.projectId).getOrThrow()
        }

        val isUpdatingDecisionMatrix = isUpdatingDecisionMatrix(fields)
        val project = if (isUpdatingDecisionMatrix) getProjectByIdOrNull(request.projectId) else null

        val settings = request.settings
        ProjectTable.updateByIdAndGet(request.projectId, ResultRow::toProject) {
            for (field in fields) {
                when (field) {
                    ProjectField.NAME -> it[ProjectTable.name] = request.name
                    ProjectField.STATUS -> it[ProjectTable.status] = request.status
                    ProjectField.SIMILARITY_THRESHOLD ->
                        it[ProjectTable.similarityThreshold] = settings.similarityThreshold
                    ProjectField.SNOWBALLING_TYPE -> it[ProjectTable.snowballingType] = settings.snowballingType
                    ProjectField.REVIEW_MAYBE_ALLOWED ->
                        it[ProjectTable.reviewMaybeAllowed] = settings.reviewMaybeAllowed
                    ProjectField.FETCHERS -> it[ProjectTable.fetchers] = settings.fetchers

                    ProjectField.NUMBER_OF_REVIEWERS,
                    ProjectField.DECISION_MATRIX_PATTERNS,
                    -> { /* decision matrix is handled below */ }
                }
            }

            if (isUpdatingDecisionMatrix && project != null) {
                it.applyDecisionMatrixUpdate(project, request.settings, fields)
            }

            it[modifiedAt] = OffsetDateTime.now()
        }
    }

    override suspend fun isProjectLocked(projectId: UUID): Boolean = db.query {
        ProjectPaperTable
            .join(ReviewTable, JoinType.INNER, ProjectPaperTable.id, ReviewTable.projectPaperId)
            .selectAll()
            .where { ProjectPaperTable.projectId eq projectId }
            .limit(1)
            .any()
    }

    override suspend fun softDeleteProject(projectId: UUID) {
        db.query {
            ProjectTable.update({ ProjectTable.id eq projectId }) {
                it[status] = ProjectStatus.DELETED
                it[deletedAt] = OffsetDateTime.now()
            }
        }
    }

    override suspend fun clearSoftDeletedProjects(thresholdDate: OffsetDateTime) = db.query {
        val clearedProjects = ProjectTable.update(
            {
                (ProjectTable.status eq ProjectStatus.DELETED).and(ProjectTable.deletedAt lessEq thresholdDate)
            },
        ) {
            it[name] = ""
            it[status] = ProjectStatus.CLEARED
            it[fetchers] = emptyMap()
            it[similarityThreshold] = 0f
            it[fetchers] = emptyMap()

            it[modifiedBy] = null
            it[modifiedAt] = OffsetDateTime.now()
            it[deletedBy] = null
            it[archivedBy] = null
        }

        logger.info { "Cleared $clearedProjects soft-deleted projects older than $thresholdDate." }
    }

    override suspend fun hardDeleteClearedProjects() {
        val projectIdsToDelete = getProjectIdsToDelete()

        if (projectIdsToDelete.isEmpty()) {
            logger.info { "No projects to hard-delete." }
            return
        }

        val (successfulDeletedIds, failedToDeleteIds) = projectIdsToDelete.partition { projectId ->
            attemptToDeleteProject(projectId)
        }

        logger.info {
            "Hard-deleted ${successfulDeletedIds.size} projects, failed to delete ${failedToDeleteIds.size} projects."
        }
    }

    override suspend fun updateMaxStageIfExceeded(projectId: UUID, stage: Int) {
        db.query {
            ProjectTable.update({ (ProjectTable.id eq projectId) and (ProjectTable.maxStage less stage) }) {
                it[ProjectTable.maxStage] = stage
            }
        }
    }

    /**
     * Retrieves a list of project IDs that are eligible for hard deletion.
     *
     * @return A list of project IDs that are eligible for hard deletion.
     */
    private suspend fun getProjectIdsToDelete(): List<UUID> = db.query {
        ProjectTable
            .selectAll()
            .where {
                (ProjectTable.status eq ProjectStatus.CLEARED).and(ProjectTable.deletedAt.isNotNull())
            }
            .map { it[ProjectTable.id].value }
    }

    /**
     * Attempts to delete a single project by its ID.
     *
     * @param projectId The ID of the project to be deleted.
     * @return `true` if the project was successfully deleted, `false` otherwise.
     */
    private suspend fun attemptToDeleteProject(projectId: UUID): Boolean = db.query {
        try {
            val deletedRows = ProjectTable.deleteWhere { ProjectTable.id eq projectId }
            deletedRows > 0
        } catch (e: ExposedSQLException) {
            logger.debug(e) { "Failed to hard-delete project $projectId, likely due to existing references." }
            false
        }
    }

    private fun isUpdatingDecisionMatrix(fields: Set<ProjectField>) = fields.any { it.isDecisionMatrixField() }

    private fun UpdateStatement.applyDecisionMatrixUpdate(
        project: Project,
        settings: UpdateProjectSettingRequest,
        fields: Set<ProjectField>,
    ) {
        var decisionMatrix = project.reviewDecisionMatrix
        if (ProjectField.NUMBER_OF_REVIEWERS in fields) {
            decisionMatrix = decisionMatrix.copy(numberOfReviewers = settings.decisionMatrix.numberOfReviewers)
        }
        if (ProjectField.DECISION_MATRIX_PATTERNS in fields) {
            decisionMatrix = decisionMatrix.copy(patterns = settings.decisionMatrix.patterns)
        }
        this[ProjectTable.reviewDecisionMatrixBinary] = decisionMatrix.toByteArray()
    }
}
