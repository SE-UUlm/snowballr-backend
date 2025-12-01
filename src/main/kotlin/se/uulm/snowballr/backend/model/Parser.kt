package se.uulm.snowballr.backend.model

import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException
import java.util.UUID

/**
 * Parses the passed [uuid] string to a UUID. If the parsing fails an [InvalidUUIDException] is thrown.
 *
 * @param uuid The UUID string to parse.
 * @param entityType The type of the entity represented by the [uuid].
 * @throws [InvalidUUIDException] If the [uuid] has not the UUID format.
 */
fun parseUUID(uuid: String, entityType: EntityType): UUID =
    runCatching { UUID.fromString(uuid) }.getOrNull() ?: throw InvalidUUIDException(entityType, uuid)
