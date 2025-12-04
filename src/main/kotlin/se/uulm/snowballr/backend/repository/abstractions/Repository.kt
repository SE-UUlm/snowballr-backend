package se.uulm.snowballr.backend.repository.abstractions

import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.EntityIDColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinOffsetDateTimeColumnType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import se.uulm.snowballr.backend.model.repository.RepoEntityId
import se.uulm.snowballr.backend.model.repository.WhereQuery
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A generic repository class that provides basic CRUD operations for entities with one or more ID columns.
 *
 * This class is designed to work with an [IdTable] and supports entities with composite primary keys.
 * It provides methods for creating, reading, updating, and deleting entities in the database.
 *
 * **Note:** This class assumes that each call is made within a transaction.
 *
 * @param TableT The table type that extends [IdTable] with the appropriate ID column type.
 * @param TableIdT The type of the ID column in the table. Usually a primitive type like [UUID] or a [CompositeID] for
 * tables with composite primary keys, i.e., an association table with an n-to-m relationship.
 * @param EntityT The type of the entity that this repository manages.
 * @param EntityIdT The type of the entity's ID, which must implement [RepoEntityId].
 * @property table The table of type [TableT] associated with this repository.
 * @property entityMapper A function that maps a [ResultRow] to an instance of [EntityT], i.e., converts a database row
 * to an entity object.
 * @property entityType The type of entity being managed, used for error reporting.
 * @property entityQuery A function that generates an [Op] expression to find an entity. This takes an [RepoEntityId]
 * and returns an [Op] that can be used in SQL queries to locate the entity.
 */
abstract class Repository<TableT : IdTable<TableIdT>, TableIdT : Any, EntityT : Any, EntityIdT : RepoEntityId>(
    protected val table: TableT,
    protected val entityMapper: IEntityMapper<EntityT>,
    protected val entityType: EntityType,
    private val entityQuery: (EntityIdT) -> WhereQuery,
) {
    /** ====== CREATE ====== */

    fun createEntity(body: TableT.(InsertStatement<EntityID<TableIdT>>) -> Unit) =
        table.insertAndGet(entityMapper, body)

    /** ====== READ ====== */

    fun <T : Any> getEntityByIdOrNull(entityId: EntityIdT, mapper: IEntityMapper<T>): T? =
        table.getEntityOrNull(mapper) { entityQuery(entityId).toOp() }

    fun <T : Any> getEntityById(entityId: EntityIdT, mapper: IEntityMapper<T>): Result<T> =
        wrapAsResult(getEntityByIdOrNull(entityId, mapper), entityId.toString())

    fun getEntityById(entityId: EntityIdT): Result<EntityT> =
        wrapAsResult(getEntityByIdOrNull(entityId, entityMapper), entityId.toString())

    fun <T : Any> getEntityByKeyOrNull(query: WhereQuery, mapper: IEntityMapper<T>): T? =
        table.getEntityOrNull(mapper) { query.toOp() }

    fun getEntityByKeyOrNull(query: WhereQuery): EntityT? = getEntityByKeyOrNull(query, entityMapper)

    fun getEntityByKey(query: WhereQuery, exception: EntityNotFoundException): Result<EntityT> =
        wrapAsResult(getEntityByKeyOrNull(query), exception)

    fun <EntT : Any> getEntityByKey(
        query: WhereQuery,
        identifierType: IdentifierType = IdentifierType.ID,
        mapper: IEntityMapper<EntT>,
    ): Result<EntT> = wrapAsResult(getEntityByKeyOrNull(query, mapper), "KEY", identifierType)

    fun getEntityByKey(query: WhereQuery, identifierType: IdentifierType = IdentifierType.ID): Result<EntityT> =
        wrapAsResult(getEntityByKeyOrNull(query), "KEY", identifierType)

    // TODO: abstract mapper to class-level generic so that the default mapper can be used as default parameter
    // -> reduce overloading

    fun <T> getAllEntitiesWhere(mapper: IEntityMapper<T>, where: SqlExpressionBuilder.() -> Op<Boolean>): List<T> =
        if (mapper is SingleValueMapper) {
            table.select(mapper.column).where(where).map(mapper::toEntity)
        } else {
            table.selectAll().where(where).map(mapper::toEntity)
        }

    fun getAllEntitiesWhere(where: SqlExpressionBuilder.() -> Op<Boolean>): List<EntityT> =
        getAllEntitiesWhere(entityMapper, where)

    fun doesEntityExistByKey(query: WhereQuery): Boolean = table.doesEntityExist { query.toOp() }

    fun doesEntityExistById(entityId: EntityIdT): Boolean = table.doesEntityExist { entityQuery(entityId).toOp() }

    /** ====== UPDATE ====== */

    /**
     * Updates an entity matching the [whereOp] expression according to the [body] and returns the updated entity.
     *
     * If the table of the entity contains "modified_at" or "modified_by" columns, these are automatically updated if
     * any changes are made to the entity.
     *
     * @param id A string representation of the entity's ID, used for error messages.
     * @param whereOp The SQL expression that is used to find the entity.
     * @param body The actual update logic that modifies the entity.
     */
    fun updateEntityById(entityId: EntityIdT, body: TableT.(UpdateStatement) -> Unit): EntityT {
        justUpdateEntityById(entityId, body)
        return getAllEntitiesWhere { entityQuery(entityId).toOp() }.single()
    }

    /**
     * Same as [updateEntityById] but does not return the updated entity.
     */
    fun justUpdateEntityById(entityId: EntityIdT, body: TableT.(UpdateStatement) -> Unit) {
        table.update({ entityQuery(entityId).toOp() }) {
            body(it)
            it.setModifiedColumnsIfRequired(UUID.randomUUID()) // TODO: Pass the actual user ID here
        }
    }

    /** ====== DELETE ====== */

    fun deleteEntityByKey(query: WhereQuery) {
        table.deleteWhere { query.toOp() }
    }

    fun deleteEntityById(entityId: EntityIdT) {
        table.deleteWhere { entityQuery(entityId).toOp() }
    }

    /** ====== RESULT WRAPPER ====== */

    private fun <T> wrapAsResult(entity: T?, exception: EntityNotFoundException): Result<T> = if (entity != null) {
        Result.success(entity)
    } else {
        Result.failure(exception)
    }

    private fun <T> wrapAsResult(
        entity: T?,
        key: String,
        identifierType: IdentifierType = IdentifierType.ID,
    ): Result<T> = wrapAsResult(entity, EntityNotFoundException(entityType, key, identifierType = identifierType))

    /** ====== HELPERS ====== */

    private fun UpdateStatement.setModifiedColumnsIfRequired(currentUserId: UUID) {
        // If no columns are being updated, we don't update the "modified_at" and "modified_by" columns
        if (this.firstDataSet.isEmpty()) {
            return
        }

        setModifiedAtIfAvailable()
        setModifiedByIfAvailable(currentUserId)
    }

    private fun UpdateStatement.setModifiedAtIfAvailable() {
        val modifiedAtColumn = table.columns.firstOrNull { column ->
            column.name == "modified_at" && column.columnType is KotlinOffsetDateTimeColumnType
        }
        if (modifiedAtColumn != null) {
            @Suppress("UNCHECKED_CAST")
            this[modifiedAtColumn as Column<OffsetDateTime>] = OffsetDateTime.now()
        }
    }

    private fun UpdateStatement.setModifiedByIfAvailable(currentUserId: UUID) {
        val modifiedByColumn = table.columns.firstOrNull { column ->
            column.name == "modified_by" && column.columnType is EntityIDColumnType<*>
        }
        if (modifiedByColumn != null) {
            // @Suppress("UNCHECKED_CAST")
            // this[modifiedByColumn as Column<EntityID<UUID>>] = currentUserId TODO: uncomment and fix
        }
    }
}
