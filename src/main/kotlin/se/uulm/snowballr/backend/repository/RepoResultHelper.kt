package se.uulm.snowballr.backend.repository

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import java.util.UUID

/**
 * Returns an entity by its ID encapsulated by a [Result]. If the entity couldn't be found a [Result.Failure] is
 * returned, containing a [NotFoundException].
 *
 * @param T The type of the entity.
 * @param getter The actual getter method that returns a nullable entity.
 * @param entityType The [EntityType] of the entity.
 * @param id The ID of the entity.
 */
fun <T> getEntityByIdAsResult(getter: (UUID) -> T?, entityType: EntityType, id: UUID): Result<T> {
    val entity = getter(id)

    return if (entity != null) {
        Result.success(entity)
    } else {
        Result.failure(NotFoundException(entityType, id.toString()))
    }
}

/**
 * Returns an entity by its two IDs encapsulated by a [Result]. If the entity couldn't be found a [Result.Failure] is
 * returned, containing a [NotFoundException].
 *
 * @param T The type of the entity.
 * @param getter The actual getter method that returns a nullable entity.
 * @param entityType The [EntityType] of the entity.
 * @param id1 The first ID of the entity.
 * @param id2 The second ID of the entity.
 */
fun <T> getEntityByIdsAsResult(getter: (UUID, UUID) -> T?, entityType: EntityType, id1: UUID, id2: UUID): Result<T> {
    val entity = getter(id1, id2)

    return if (entity != null) {
        Result.success(entity)
    } else {
        Result.failure(NotFoundException(entityType, id1.toString(), id2.toString()))
    }
}
