package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
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
 */
fun UUIDTable.getEntityId(id: UUID): EntityID<UUID>? = this
    .select(this.id)
    .where { this@getEntityId.id eq id }
    .map { it[this.id] }
    .singleOrNull()

/**
 * Returns the entity ID of the user with the passed [id] or throws a [NotFoundException.User] if the user doesn't
 * exist.
 *
 * @see getEntityId
 */
fun getUserEntityId(id: UUID): EntityID<UUID> = UserTable.getEntityId(id) ?: throw NotFoundException.User(id.toString())

/**
 * Returns the entity ID of the project with the passed [id] or throws a [NotFoundException.Project] if the project
 * doesn't exist.
 *
 * @see getEntityId
 */
fun getProjectEntityId(id: UUID): EntityID<UUID> =
    ProjectTable.getEntityId(id) ?: throw NotFoundException.Project(id.toString())
