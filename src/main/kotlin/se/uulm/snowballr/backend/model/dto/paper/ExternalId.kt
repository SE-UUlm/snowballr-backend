package se.uulm.snowballr.backend.model.dto.paper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalId(
    @SerialName("type")
    val type: ExternalIdType,
    @SerialName("value")
    val value: String,
)

/**
 * Returns this [ExternalId] with its value trimmed and converted to the [canonical case][ExternalIdType.canonicalCase]
 * of its [type][ExternalId.type]. Two external IDs referring to the same entity are equal after normalization.
 */
fun ExternalId.normalized(): ExternalId {
    val trimmed = value.trim()
    val cased = when (type.canonicalCase) {
        CanonicalCase.AS_IS -> trimmed
        CanonicalCase.LOWER -> trimmed.lowercase()
        CanonicalCase.UPPER -> trimmed.uppercase()
    }
    return copy(value = cased)
}
