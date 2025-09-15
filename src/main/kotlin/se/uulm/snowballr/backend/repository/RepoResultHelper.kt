package se.uulm.snowballr.backend.repository

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException

/**
 * Returns an entity by its key encapsulated by a [Result]. If the entity couldn't be found, a [Result.Failure] is
 * returned, containing a [NotFoundException].
 *
 * @param Key The type of the key.
 * @param EntT The type of the entity.
 * @param getter The actual getter method that returns a nullable entity.
 * @param entityType The [EntityType] of the entity.
 * @param key The key of the entity.
 * @param identifierType The [IdentifierType] of the [key].
 */
fun <Key : Any, EntT> getEntityByKeyAsResult(
    getter: (Key) -> EntT?,
    entityType: EntityType,
    key: Key,
    identifierType: IdentifierType = IdentifierType.ID,
): Result<EntT> {
    val entity = getter(key)

    return if (entity != null) {
        Result.success(entity)
    } else {
        Result.failure(NotFoundException(entityType, key.toString(), identifierType = identifierType))
    }
}

/**
 * Returns an entity by its two keys encapsulated by a [Result]. If the entity couldn't be found, a [Result.Failure] is
 * returned, containing a [NotFoundException].
 *
 * @param Key The type of the key.
 * @param EntT The type of the entity.
 * @param getter The actual getter method that returns a nullable entity.
 * @param entityType The [EntityType] of the entity.
 * @param key1 The first key of the entity.
 * @param key2 The second key of the entity.
 */
fun <Key : Any, EntT> getEntityByKeysAsResult(
    getter: (Key, Key) -> EntT?,
    entityType: EntityType,
    key1: Key,
    key2: Key,
): Result<EntT> {
    val entity = getter(key1, key2)

    return if (entity != null) {
        Result.success(entity)
    } else {
        Result.failure(NotFoundException(entityType, key1.toString(), key2.toString()))
    }
}
