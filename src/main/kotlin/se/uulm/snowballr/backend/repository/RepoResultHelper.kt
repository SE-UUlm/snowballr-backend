package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcResult
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.SnowballRException
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException

/**
 * Returns an entity by its key encapsulated by a [Result]. If the entity couldn't be found, a [Result.Failure] is
 * returned, containing a [NotFoundException].
 *
 * @param KeyT The type of the key.
 * @param EntT The type of the entity.
 * @param getter The actual getter method that returns a nullable entity.
 * @param entityType The [EntityType] of the entity.
 * @param key The key of the entity.
 * @param identifierType The [IdentifierType] of the [key].
 */
fun <KeyT : Any, EntT> getEntityByKeyAsResult(
    getter: (KeyT) -> EntT?,
    entityType: EntityType,
    key: KeyT,
    identifierType: IdentifierType = IdentifierType.ID,
): Result<EntT> {
    val entity = getter(key)
    return wrapAsResult(entity, EntityNotFoundException(entityType, key, identifierType = identifierType))
}

/**
 * Returns an entity by its two keys encapsulated by a [Result]. If the entity couldn't be found, a [Result.Failure] is
 * returned, containing a [NotFoundException].
 *
 * @param KeyT The type of the key.
 * @param EntT The type of the entity.
 * @param getter The actual getter method that returns a nullable entity.
 * @param entityType The [EntityType] of the entity.
 * @param key1 The first key of the entity.
 * @param key2 The second key of the entity.
 */
fun <KeyT : Any, EntT> getEntityByKeysAsResult(
    getter: (KeyT, KeyT) -> EntT?,
    entityType: EntityType,
    key1: KeyT,
    key2: KeyT,
): Result<EntT> {
    val entity = getter(key1, key2)
    return wrapAsResult(entity, EntityNotFoundException(entityType, key1, key2))
}

/**
 * Wraps the given entity in a [Result] if it's not null, otherwise returns a [Result.Failure] containing the given
 * [exception].
 */
fun <T> wrapAsResult(entity: T?, exception: SnowballRException): Result<T> = if (entity != null) {
    Result.success(entity)
} else {
    Result.failure(exception)
}

/**
 * Extracts and converts rows from a [JdbcResult] to a list of object of type [T].
 *
 * @param T The table entity type.
 * @param result The [JdbcResult] containing the entity data.
 * @param table The table from which the result originates.
 * @param mapper The function that transforms a [ResultRow] to an object of type [T].
 * @return A list of entity objects of type [T] extracted from the result set.
 */
fun <T> extractTableRows(result: JdbcResult, table: Table, mapper: (ResultRow) -> T): List<T> = generateSequence {
    if (result.next()) {
        ResultRow.create(
            result,
            table.fields.withIndex().associate { it.value to it.index },
        )
    } else {
        null
    }
}.map(mapper).toList()
