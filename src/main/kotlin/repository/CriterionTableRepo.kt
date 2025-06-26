package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.CriterionTable.toCriterion
import se.uulm.snowballr.backend.table.getProjectEntityId
import se.uulm.snowballr.backend.table.getUserEntityId
import snowballr.CriterionOuterClass

/**
 * Defines an interface for repository operations related to the [CriterionTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for criteria. By using this interface, the functionality for creating
 * criteria can remain decoupled from the specifics of the database layer.
 */
interface ICriterionTableRepo {
    suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create, userId: String,): Criterion
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
    override suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create, userId: String,): Criterion =
        db.dbQuery {
            // Get user reference
            val userEntityId = getUserEntityId(userId)

            // Get project reference
            val projectEntityId = getProjectEntityId(request.projectId)

            // Create criterion
            val criterionId =
                CriterionTable
                    .insertAndGetId {
                        it[tag] = request.tag
                        it[name] = request.name
                        it[description] = request.description
                        it[category] = request.category
                        it[projectId] = projectEntityId
                        it[createdBy] = userEntityId
                    }

            // Return created criterion
            CriterionTable
                .selectAll()
                .andWhere { CriterionTable.id eq criterionId }
                .map { it.toCriterion() }
                .singleOrNull()
                ?: throw EntityNotPersistedException.Criterion(criterionId.toString())
        }
}
