package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.getUserEntityId
import se.uulm.snowballr.backend.table.toProject
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [ProjectTable].
 *
 * This interface is used to handle persistence and retrieval operations for projects by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing projects can remain decoupled from the specifics of the database layer.
 */
interface IProjectTableRepo {
    /**
     * Creates a new project in the database with the provided project creation request and user ID.
     *
     * @param request The project creation request containing project details
     * @param userId The ID of the user creating the project.
     * @return The created [Project] object representing the newly created project.
     */
    suspend fun createProject(request: ProjectOuterClass.Project.Create, userId: UUID): Project

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
     * By default, [ProjectStatus.PROJECT_STATUS_ACTIVE] and [ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED] are used,
     * which includes both active and active-locked projects, i.e., projects where settings cannot be changed anymore.
     * If set to another value (e.g., [ProjectStatus.PROJECT_STATUS_DELETED]), only projects
     * matching one of the statuses (e.g., deleted) will be returned.
     *
     * @return A list of [Project] objects matching the specified filters where the given user is member of.
     */
    suspend fun getUserProjects(
        userId: UUID,
        statusFilters: Set<ProjectStatus> = setOf(
            ProjectStatus.PROJECT_STATUS_ACTIVE,
            ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
        ),
    ): List<Project>
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
class ProjectTableRepo(
    private val db: IDatabase,
) : IProjectTableRepo {
    override suspend fun createProject(request: ProjectOuterClass.Project.Create, userId: UUID): Project = db.dbQuery {
        // Get user reference
        val userEntityId = getUserEntityId(userId)

        ProjectTable.insertAndGet(ResultRow::toProject, EntityType.PROJECT) {
            it[name] = request.name
            it[status] = ProjectStatus.PROJECT_STATUS_ACTIVE
            it[currentStage] = 0
            it[maxStage] = 0
            // TODO: Fetch default settings from user
            it[similarityThreshold] = 0F
            it[snowballingType] = SnowballingType.SNOWBALLING_TYPE_BOTH
            it[reviewMaybeAllowed] = true
            it[reviewDecisionMatrixBinary] = ReviewDecisionMatrix.getDefaultInstance().toByteArray()
            it[fetcherApis] = emptyList()
            it[createdBy] = userEntityId
        }
    }

    override suspend fun getAllProjects(): List<Project> = db.dbQuery {
        ProjectTable
            .selectAll()
            .where {
                (ProjectTable.status eq ProjectStatus.PROJECT_STATUS_ACTIVE) or
                    (ProjectTable.status eq ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)
            }
            .map { it.toProject() }
    }

    override suspend fun getUserProjects(userId: UUID, statusFilters: Set<ProjectStatus>): List<Project> = db.dbQuery {
        val excludedStatuses = listOf(ProjectStatus.PROJECT_STATUS_UNSPECIFIED)
        val projectFilter = statusFilters
            .filterNot { it in excludedStatuses }
            .map { ProjectTable.status eq it }
            .reduceOrNull { acc, filter -> acc or filter }

        require(
            projectFilter != null,
        ) { "Unsupported filter statuses: ${statusFilters.joinToString(", ") { it.name }}." }

        (ProjectTable innerJoin ProjectMemberTable)
            .select(ProjectTable.columns)
            .where { ProjectMemberTable.userId eq userId }
            .andWhere { projectFilter }
            .map { it.toProject() }
    }
}
