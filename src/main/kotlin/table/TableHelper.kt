package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.parseUUID
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
 * @return The ID of the entity as [EntityID], or null if no entity exists.
 * @throws InvalidIdException.UUID If [id] cannot be parsed to a UUID.
 */
fun UUIDTable.getEntityId(id: String): EntityID<UUID>? {
    val uuid = parseUUID(id, "user")

    return this
        .select(this.id)
        .where { this@getEntityId.id eq uuid }
        .map { it[this.id] }
        .singleOrNull()
}
