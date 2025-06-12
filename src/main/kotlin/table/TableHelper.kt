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
 */
fun IntIdTable.getEntityId(id: String): EntityID<Int>? =
    this
        .select(this.id)
        .where { this@getEntityId.id eq id.toInt() }
        .map { it[this.id] }
        .singleOrNull()

/**
 * @see IntIdTable.getEntityId
 */
fun UUIDTable.getEntityId(id: String): EntityID<UUID>? =
    this
        .select(this.id)
        .where { this@getEntityId.id eq UUID.fromString(id) }
        .map { it[this.id] }
        .singleOrNull()
