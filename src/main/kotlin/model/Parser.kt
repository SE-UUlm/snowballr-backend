package se.uulm.snowballr.backend.model

import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import java.util.UUID

/**
 * Parses the passed [uuid] string to a UUID. If the parsing fails an [InvalidIdException.UUID] is thrown.
 */
fun parseUUID(
    uuid: String,
    entityType: String,
): UUID = runCatching { UUID.fromString(uuid) }.getOrNull() ?: throw InvalidIdException.UUID(uuid, entityType)

/**
 * Parses the passed [id] string to an integer ID. If the parsing fails an [InvalidIdException.IntId] is thrown.
 */
fun parseIntId(
    id: String,
    entityType: String,
): Int {
    val intId = id.toIntOrNull()

    if (intId == null || intId < 0) {
        throw InvalidIdException.IntId(id, entityType)
    }

    return intId
}
