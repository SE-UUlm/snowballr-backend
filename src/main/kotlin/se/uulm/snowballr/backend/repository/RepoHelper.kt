package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import java.util.UUID

/**
 * Returns an entity according to the [where] expression or returns null if the entity couldn't be found.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 * @param where The SQL expression that is used to find the entity.
 */
fun <Key : Any, T : IdTable<Key>, EntT : Any> T.getEntityOrNull(
    mapper: (ResultRow) -> EntT,
    where: SqlExpressionBuilder.() -> Op<Boolean>,
): EntT? = this.selectAll()
    .where(where)
    .map(mapper)
    .singleOrNull()

/**
 * Returns an entity by its ID or returns null if the entity couldn't be found.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param id The ID of type [Key], which is used to find the entity.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 */
fun <Key : Any, T : IdTable<Key>, EntT : Any> T.getEntityByIdOrNull(id: Key, mapper: (ResultRow) -> EntT): EntT? =
    this.getEntityOrNull(mapper) { this@getEntityByIdOrNull.id eq id }

/**
 * Combination of using [insertAndGetId] and fetching the created entity by its ID.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 * @param entityType The entity type used for the [EntityNotPersistedException], which is thrown when the entity cannot
 * be retrieved by its ID.
 * @param body The body that is passed to [insertAndGetId].
 */
inline fun <Key : Any, T : IdTable<Key>, EntT : Any> T.insertAndGet(
    noinline mapper: (ResultRow) -> EntT,
    entityType: EntityType,
    crossinline body: T.(InsertStatement<EntityID<Key>>) -> Unit,
): EntT {
    val id = this.insertAndGetId(body).value
    return this.getEntityByIdOrNull(id, mapper) ?: throw EntityNotPersistedException(entityType, id.toString())
}

/**
 * Combination of using [update] and fetching the updated entity by its ID.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param id The ID of type [Key], which is used to find the entity that should be updated.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 * @param entityType The entity type used for the [EntityNotPersistedException], which is thrown when the entity cannot
 * be retrieved by its ID.
 * @param body The body that is passed to [update].
 */
inline fun <Key : Any, T : IdTable<Key>, EntT : Any> T.updateByIdAndGet(
    id: Key,
    noinline mapper: (ResultRow) -> EntT,
    entityType: EntityType,
    crossinline body: T.(UpdateStatement) -> Unit,
): EntT {
    this.update({ this@updateByIdAndGet.id eq id }, body = body)
    return this.getEntityByIdOrNull(id, mapper) ?: throw EntityNotPersistedException(entityType, id.toString())
}
