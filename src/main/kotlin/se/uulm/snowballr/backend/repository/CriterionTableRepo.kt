package se.uulm.snowballr.backend.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.CriterionField
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.toCriterion
import se.uulm.snowballr.backend.table.toProjectCriterion
import se.uulm.snowballr.backend.table.toUserCriterion
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Defines an interface for repository operations related to the [CriterionTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for criteria. By using this interface, the functionality for creating
 * criteria can remain decoupled from the specifics of the database layer.
 */
interface ICriterionTableRepo {
    /**
     * Returns a [Result] containing the criterion by its ID or a [NotFoundException] if the criterion with the passed
     * [id] doesn't exist.
     */
    suspend fun getCriterionById(id: UUID): Result<Criterion>

    /**
     * Creates a new criterion in the database based on the provided request and user ID.
     *
     * @param request The creation request containing details for the new criterion.
     * @param userId The ID of the user creating the criterion.
     * @return The created [Criterion] object representing the newly created criterion.
     */
    suspend fun createCriterion(request: CreateCriterionRequest, userId: UUID): Criterion

    /**
     * Retrieves all criteria associated with a specific user.
     *
     * @param userId The unique identifier of the user for whom the criteria are being retrieved.
     * @return A list of [Criterion] objects associated with the given user.
     */
    suspend fun getAllUserCriteria(userId: UUID): List<Criterion>

    /**
     * Updates an existent criterion in the database with the provided new information.
     *
     * @param request The update request containing the new criterion details, such as the new name.
     * @param paths The field mask paths that should be updated.
     * @return The updated [Criterion] object reflecting the changes from the [request].
     */
    suspend fun updateCriterion(request: UpdateCriterionRequest, paths: List<CriterionField>): Criterion

    /**
     * Deletes a list of criteria from the database based on their IDs.
     *
     * @param ids A list of [UUID]s for the criteria to be deleted.
     */
    suspend fun deleteCriteriaByIds(ids: List<UUID>)

    /**
     * Deletes all user criteria associated with a specific user.
     *
     * @param userId The unique identifier of the user whose criteria are to be deleted.
     */
    suspend fun deleteUserCriteriaByUserId(userId: UUID)

    /**
     * Retrieves a list of criteria based on their unique identifiers.
     *
     * @param ids A list of unique identifiers (UUIDs) for the criteria to be fetched.
     * @return A list of [Criterion] objects corresponding to the provided IDs.
     *         If no criteria are found for certain IDs, they may be excluded from the result.
     */
    suspend fun getCriteriaByIds(ids: List<UUID>): List<Criterion>

    /**
     * Retrieves all criteria associated with a specific project.
     *
     * @param projectId The unique identifier of the project for which the criteria are being retrieved.
     * @return A list of [Criterion] objects associated with the given project.
     */
    suspend fun getAllProjectCriteria(projectId: UUID): List<Criterion.ProjectCriterion>
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
    private fun getCriterionByIdOrNull(id: UUID): Criterion? =
        CriterionTable.getEntityByIdOrNull(id, ResultRow::toCriterion)

    override suspend fun getCriterionById(id: UUID): Result<Criterion> = db.query {
        getEntityByKeyAsResult(::getCriterionByIdOrNull, EntityType.CRITERION, id)
    }

    override suspend fun createCriterion(request: CreateCriterionRequest, userId: UUID): Criterion = db.query {
        CriterionTable.insertAndGet(ResultRow::toCriterion) {
            it[tag] = request.tag
            it[name] = request.name
            it[description] = request.description
            it[category] = request.category
            it[projectId] = request.projectId
            it[createdBy] = userId
        }
    }

    override suspend fun updateCriterion(request: UpdateCriterionRequest, paths: List<CriterionField>): Criterion =
        db.query {
            CriterionTable.updateByIdAndGet(request.criterionId, ResultRow::toCriterion) {
                for (field in paths) {
                    when (field) {
                        CriterionField.TAG -> it[tag] = request.tag
                        CriterionField.NAME -> it[name] = request.name
                        CriterionField.DESCRIPTION -> it[description] = request.description
                        CriterionField.CATEGORY -> it[category] = request.category
                    }
                }
            }
        }

    override suspend fun deleteCriteriaByIds(ids: List<UUID>) {
        val deletedIds = db.query {
            CriterionTable.deleteWhere { CriterionTable.id inList ids }
        }

        logger.info { "Deleted $deletedIds criteria." }
    }

    override suspend fun deleteUserCriteriaByUserId(userId: UUID) {
        val deletedIds = db.query {
            CriterionTable.deleteWhere {
                (CriterionTable.createdBy eq userId).and(CriterionTable.projectId.isNull())
            }
        }

        logger.info { "Deleted $deletedIds user criteria." }
    }

    override suspend fun getCriteriaByIds(ids: List<UUID>): List<Criterion> = db.query {
        CriterionTable.getEntitiesByIds(ids, ResultRow::toCriterion)
    }

    override suspend fun getAllUserCriteria(userId: UUID): List<Criterion.UserCriterion> = db.query {
        CriterionTable.getEntities(ResultRow::toUserCriterion) {
            CriterionTable.createdBy eq userId and CriterionTable.projectId.isNull()
        }
    }

    override suspend fun getAllProjectCriteria(projectId: UUID): List<Criterion.ProjectCriterion> = db.query {
        CriterionTable.getEntities(ResultRow::toProjectCriterion) {
            CriterionTable.projectId eq projectId
        }
    }
}
