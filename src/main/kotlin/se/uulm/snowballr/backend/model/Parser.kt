package se.uulm.snowballr.backend.model

import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import java.util.UUID

/**
 * Parses the passed [uuid] string to a UUID. If the parsing fails an [InvalidIdException.UUID] is thrown.
 */
fun parseUUID(uuid: String, entityType: EntityType): UUID =
    runCatching { UUID.fromString(uuid) }.getOrNull() ?: throw InvalidIdException.UUID(uuid, entityType)
