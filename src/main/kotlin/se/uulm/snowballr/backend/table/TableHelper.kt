package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import java.util.UUID

/**
 * Returns the entity ID of the entity with the given [id] or throws a [NotFoundException] if no such entity exists.
 * This can be used to reference the entity in other table rows.
 *
 * Example:
 * Table A stores a reference to this table as `entity_id`. To create a row in table A, we can use this method
 * to get the [EntityID] and then pass it to the `entity_id` column of table A.
 *
 * @param id The ID of the entity as [String].
 * @param entityType The type of the entity.
 * @return The ID of the entity as [EntityID].
 */
private fun UUIDTable.getEntityId(id: UUID, entityType: EntityType): EntityID<UUID> = this
    .select(this.id)
    .where { this@getEntityId.id eq id }
    .map { it[this.id] }
    .singleOrNull()
    ?: throw NotFoundException(entityType, id.toString())

/**
 * Returns the entity ID of the user with the passed [id] or throws a [NotFoundException] if the user doesn't
 * exist.
 *
 * @see getEntityId
 */
fun getUserEntityId(id: UUID): EntityID<UUID> = UserTable.getEntityId(id, EntityType.USER)

/**
 * Returns the entity ID of the project with the passed [id] or throws a [NotFoundException] if the project
 * doesn't exist.
 *
 * @see getEntityId
 */
fun getProjectEntityId(id: UUID): EntityID<UUID> = ProjectTable.getEntityId(id, EntityType.PROJECT)
