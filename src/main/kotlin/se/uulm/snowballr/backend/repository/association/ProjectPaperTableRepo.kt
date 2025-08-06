package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.repository.getEntityByIdOrNull
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.toProjectPaper
import se.uulm.snowballr.backend.table.association.toProjectPaperWithPaper
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [ProjectPaperTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for project papers. By using this interface, the functionality for creating
 * project papers can remain decoupled from the specifics of the database layer.
 */
interface IProjectPaperTableRepo {
    /**
     * Returns a project paper by its ID or throws a [NotFoundException] if the project paper with the passed [id] doesn't exist.
     */
    suspend fun getProjectPaperById(id: UUID): ProjectPaper

    /**
     * Retrieves a list of project papers along with their associated papers for the specified project.
     *
     * This method fetches data that combines information from a `ProjectPaper` and its related `Paper`
     * for a given project, represented by its unique identifier.
     *
     * @param projectId The unique identifier of the project for which the project papers and
     *                  their associated papers should be retrieved.
     * @return A list of [ProjectPaperWithPaper] instances, each representing a relationship
     *         between a project paper and its associated paper.
     */
    suspend fun getProjectPapersWithPapers(projectId: UUID): List<ProjectPaperWithPaper>
}

/**
 * Repository implementation for managing the [ProjectPaperTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations
 * for project paper data by leveraging the database abstraction defined in [IDatabase]. It
 * facilitates CRUD operations on project paper records associated with a given project and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class ProjectPaperTableRepo(
    private val db: IDatabase,
) : IProjectPaperTableRepo {
    private fun getProjectPaperByIdOrNull(id: UUID): ProjectPaper? = ProjectPaperTable.getEntityByIdOrNull(
        id,
        ResultRow::toProjectPaper,
    )

    override suspend fun getProjectPaperById(id: UUID): ProjectPaper = db.query {
        getProjectPaperByIdOrNull(id) ?: throw NotFoundException(EntityType.PROJECT_PAPER, id.toString())
    }

    override suspend fun getProjectPapersWithPapers(projectId: UUID): List<ProjectPaperWithPaper> = db.query {
        ProjectPaperTable
            .join(ProjectTable, JoinType.INNER, onColumn = ProjectPaperTable.projectId, otherColumn = ProjectTable.id)
            .join(PaperTable, JoinType.INNER, onColumn = ProjectPaperTable.paperId, otherColumn = PaperTable.id)
            .selectAll()
            .where { ProjectPaperTable.projectId eq projectId }
            .map { it.toProjectPaperWithPaper() }
    }
}
