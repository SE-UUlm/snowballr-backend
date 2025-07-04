package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import java.util.UUID

/**
 * Combination of using [insertAndGetId] and fetching the created object by its ID.
 *
 * @param Key The type of the [IdTable], i.e., the ID type, such as [UUID].
 * @param T The table type as a subtype of [IdTable].
 * @param EntT The result entity type.
 * @param mapper Mapping function of the [ResultRow] to the entity type [EntT].
 * @param getException Getter method for the subtype of [EntityNotPersistedException], which is thrown when the entity
 * cannot be retrieved by its ID.
 * @param body The body that is passed to [insertAndGetId].
 */
inline fun <Key : Any, T : IdTable<Key>, EntT : Any> T.insertAndGet(
    mapper: (ResultRow) -> EntT,
    getException: (String) -> EntityNotPersistedException,
    crossinline body: T.(InsertStatement<EntityID<Key>>) -> Unit,
): EntT {
    val id = this.insertAndGetId(body).value
    return this
        .selectAll()
        .where { this@insertAndGet.id eq id }
        .map { mapper.invoke(it) }
        .singleOrNull()
        ?: throw getException(id.toString())
}
