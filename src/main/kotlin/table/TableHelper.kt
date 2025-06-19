package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

/**
 * Returns the entity ID of the entity with the given [id] or `null` if no such entity exists.
 * This can be used to reference the entity in other table rows.
 *
 * Example:
 * Table A stores a reference to this table as `entity_id`. To create a row in table A, we can use this method
 * to get the [EntityID] and then pass it to the `entity_id` column of table A.
 *
 * @param id The ID of the entity as [String].
 * @return The ID of the entity as [EntityID], or null if no entity exists or the [id] couldn't be parsed to an [Int].
 */
fun IntIdTable.getEntityId(id: String): EntityID<Int>? {
    val intId = id.toIntOrNull() ?: return null

    return this
        .select(this.id)
        .where { this@getEntityId.id eq intId }
        .map { it[this.id] }
        .singleOrNull()
}

/**
 * Same as [IntIdTable.getEntityId], but for the [UUIDTable] and thus the [UUID] type.
 *
 * @see IntIdTable.getEntityId
 *
 * @param id The ID of the entity as [String].
 * @return The ID of the entity as [EntityID], or null if no entity exists or the [id] couldn't be parsed to a [UUID].
 */
fun UUIDTable.getEntityId(id: String): EntityID<UUID>? {
    val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return null

    return this
        .select(this.id)
        .where { this@getEntityId.id eq uuid }
        .map { it[this.id] }
        .singleOrNull()
}
