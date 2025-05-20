package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.FetcherApi
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ProjectTable.toProject
import snowballr.ProjectOuterClass

/**
 * Defines an interface for repository operations related to the [ProjectTable].
 *
 * This interface is used to handle persistence and retrieval operations for projects by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing projects can remain decoupled from the specifics of the database layer.
 */
interface IProjectTableRepo {
    suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project
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
    override suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project =
        db.dbQuery {
            // Create project
            val projectId =
                ProjectTable
                    .insertAndGetId {
                        it[name] = request.name
                        it[status] = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
                        it[currentStage] = 0
                        it[maxStage] = 0
                        // Fetch default settings from user
                        it[similarityThreshold] = 0F
                        it[snowballingType] = ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH
                        it[reviewMaybeAllowed] = true
                        it[reviewDecisionMatrixBinary] =
                            ProjectOuterClass.ReviewDecisionMatrix.getDefaultInstance().toByteArray()
                        it[fetcherApis] = FetcherApi.entries.toList()
                    }.value

            // Return created project
            ProjectTable
                .selectAll()
                .andWhere { ProjectTable.id eq projectId }
                .single()
                .toProject()
        }
}
