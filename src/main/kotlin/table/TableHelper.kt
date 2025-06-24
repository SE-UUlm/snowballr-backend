package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
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
 * @param entityType The type of the entity.
 * @return The ID of the entity as [EntityID], or null if no entity exists.
 * @throws InvalidIdException.UUID If [id] cannot be parsed to a UUID.
 */
private fun UUIDTable.getEntityId(
    id: String,
    entityType: String,
): EntityID<UUID>? {
    val uuid = parseUUID(id, entityType)

    return this
        .select(this.id)
        .where { this@getEntityId.id eq uuid }
        .map { it[this.id] }
        .singleOrNull()
}

/**
 * Returns the entity ID of the user with the passed [id] or throws a [NotFoundException.User] if the user doesn't
 * exist.
 *
 * @see getEntityId
 */
fun getUserEntityId(id: String): EntityID<UUID> = UserTable.getEntityId(id, "user") ?: throw NotFoundException.User(id)

/**
 * Returns the entity ID of the project with the passed [id] or throws a [NotFoundException.Project] if the project
 * doesn't exist.
 *
 * @see getEntityId
 */
fun getProjectEntityId(id: String): EntityID<UUID> =
    ProjectTable.getEntityId(id, "project") ?: throw NotFoundException.Project(id)
