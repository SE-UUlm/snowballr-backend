package se.uulm.snowballr.backend.model

import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import java.util.UUID

/**
 * Parses the passed [uuid] string to a UUID. If the parsing fails an [InvalidIdException.UUID] is thrown.
 *
 * @param uuid The UUID string to parse.
 * @param entityType The type of the entity represented by the [uuid].
 * @throws [InvalidIdException.UUID] If the [uuid] has not the UUID format.
 */
fun parseUUID(uuid: String, entityType: EntityType): UUID =
    runCatching { UUID.fromString(uuid) }.getOrNull() ?: throw InvalidIdException.UUID(entityType, uuid)
