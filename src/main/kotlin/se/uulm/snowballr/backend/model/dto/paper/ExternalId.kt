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
