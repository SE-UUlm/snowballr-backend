package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.getProjectEntityId
import se.uulm.snowballr.backend.table.getUserEntityId
import se.uulm.snowballr.backend.table.toCriterion
import snowballr.CriterionOuterClass
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [CriterionTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for criteria. By using this interface, the functionality for creating
 * criteria can remain decoupled from the specifics of the database layer.
 */
interface ICriterionTableRepo {
    /**
     * Creates a new criterion in the database based on the provided request and user ID.
     *
     * @param request The creation request containing details for the new criterion.
     * @param userId The ID of the user creating the criterion.
     * @return The created [Criterion] object representing the newly created criterion.
     */
    suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create, userId: UUID): Criterion
}

/**
 * Repository implementation for managing the [CriterionTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations
 * for criteria data by leveraging the database abstraction defined in [IDatabase]. It
 * facilitates CRUD operations on criterion records associated with a given project and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class CriterionTableRepo(
    private val db: IDatabase,
) : ICriterionTableRepo {
    override suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create, userId: UUID): Criterion =
        db.dbQuery {
            val projectUUID = parseUUID(request.projectId, "project")

            // Get user reference
            val userEntityId = getUserEntityId(userId)

            // Get project reference
            val projectEntityId = getProjectEntityId(projectUUID)

            CriterionTable.insertAndGet(ResultRow::toCriterion, { EntityNotPersistedException.Criterion(it) }) {
                it[tag] = request.tag
                it[name] = request.name
                it[description] = request.description
                it[category] = request.category
                it[projectId] = projectEntityId
                it[createdBy] = userEntityId
            }
        }
}
