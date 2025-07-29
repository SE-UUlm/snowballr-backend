package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
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
     * Returns a criterion by its ID or throws a [NotFoundException] if the criterion with the passed [id] doesn't exist.
     */
    suspend fun getCriterionById(id: UUID): Criterion

    /**
     * Creates a new criterion in the database based on the provided request and user ID.
     *
     * @param request The creation request containing details for the new criterion.
     * @param userId The ID of the user creating the criterion.
     * @return The created [Criterion] object representing the newly created criterion.
     */
    suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create, userId: UUID): Criterion

    /**
     * Updates an existing criterion in the database with the provided new information.
     * The following fields can be updated:
     * - tag
     * - name
     * - description
     * - category
     *
     * @param request The update request containing the new criterion details, such as the new name.
     * @return The updated [Criterion] object reflecting the changes from the [request].
     */
    suspend fun updateCriterion(request: CriterionOuterClass.Criterion.Update): Criterion
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
    /**
     * Requesting a criterion from the database.
     *
     * @param id The id of the requested criterion.
     * @return The [Criterion] object or null, if no criterion with the given [id] was found.
     */
    private fun getCriterionByIdOrNull(id: UUID): Criterion? = CriterionTable
        .selectAll()
        .where { CriterionTable.id eq id }
        .map { it.toCriterion() }
        .singleOrNull()

    override suspend fun getCriterionById(id: UUID): Criterion = db.query {
        getCriterionByIdOrNull(id) ?: throw NotFoundException(EntityType.CRITERION, id.toString())
    }

    override suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create, userId: UUID): Criterion =
        db.query {
            val projectUUID = parseUUID(request.projectId, EntityType.PROJECT)

            // Get user reference
            val userEntityId = getUserEntityId(userId)

            // Get project reference
            val projectEntityId = getProjectEntityId(projectUUID)

            CriterionTable.insertAndGet(ResultRow::toCriterion, EntityType.CRITERION) {
                it[tag] = request.tag
                it[name] = request.name
                it[description] = request.description
                it[category] = request.category
                it[projectId] = projectEntityId
                it[createdBy] = userEntityId
            }
        }

    override suspend fun updateCriterion(request: CriterionOuterClass.Criterion.Update): Criterion = db.query {
        val criterionId = parseUUID(request.criterion.id, EntityType.CRITERION)
        val fieldMask = FieldMaskUtil.normalize(request.mask)

        CriterionTable.updateAndGet(criterionId, ResultRow::toCriterion, EntityType.CRITERION) {
            for (field in fieldMask.pathsList) {
                when (field) {
                    "criterion.tag" -> it[tag] = request.criterion.tag
                    "criterion.name" -> it[name] = request.criterion.name
                    "criterion.description" -> it[description] = request.criterion.description
                    "criterion.category" -> it[category] = request.criterion.category
                }
            }
        }
    }
}
