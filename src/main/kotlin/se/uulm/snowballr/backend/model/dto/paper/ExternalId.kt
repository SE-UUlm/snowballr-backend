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
 * Returns this [ExternalId] with its value trimmed and, if its [type][ExternalId.type] is
 * [case-insensitive][ExternalIdType.isCaseInsensitive], lowercased. Two external IDs referring to the same entity are
 * equal after normalization.
 */
fun ExternalId.normalized(): ExternalId {
    val trimmed = value.trim()
    return copy(value = if (type.isCaseInsensitive) trimmed.lowercase() else trimmed)
}
