package se.uulm.snowballr.backend.repository.abstractions

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
import java.util.UUID

/**
 * Returns a list of entities according to the [where] expression.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param mapper Mapping function to map each [ResultRow] to an entity of type [EntT].
 * @param where The SQL expression that is used to find the entities.
 */
fun <Key : Any, T : IdTable<Key>, EntT : Any> T.getEntities(
    mapper: IEntityMapper<EntT>,
    where: SqlExpressionBuilder.() -> Op<Boolean>,
): List<EntT> = this.selectAll()
    .where(where)
    .map(mapper::toEntity)

/**
 * Combination of using [insertAndGetId] and fetching the created entity by its ID.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 * @param body The body that is passed to [insertAndGetId].
 */
inline fun <Key : Any, T : IdTable<Key>, EntT : Any> T.insertAndGet(
    mapper: IEntityMapper<EntT>,
    crossinline body: T.(InsertStatement<EntityID<Key>>) -> Unit,
): EntT {
    val id = this.insertAndGetId(body).value
    return this.getEntities(mapper) { this@insertAndGet.id eq id }.single()
}

/**
 * Checks if an entity exists in the table that matches the specified [where] condition.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param where The condition used to filter the entities in the table.
 *              It is expressed as an [Op] built using the [SqlExpressionBuilder].
 * @return `true` if an entity matching the condition exists, otherwise `false`.
 */
fun <Key : Any, T : IdTable<Key>> T.doesEntityExist(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean =
    this.selectAll().where(where).count() > 0

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
    mapper: IEntityMapper<EntT>,
    where: SqlExpressionBuilder.() -> Op<Boolean>,
): EntT? = this
    .getEntities(mapper, where)
    .singleOrNull()

/**
 * Combination of using [update] and fetching the updated entity both according to the [where] expression.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 * @param where The SQL expression that is used to find the entity.
 * @param body The body that is passed to [update].
 */
inline fun <Key : Any, T : IdTable<Key>, EntT : Any> T.updateAndGet(
    mapper: IEntityMapper<EntT>,
    noinline where: SqlExpressionBuilder.() -> Op<Boolean>,
    crossinline body: T.(UpdateStatement) -> Unit,
): EntT {
    this.update(where, body = body)
    return this.getEntities(mapper, where).single()
}
