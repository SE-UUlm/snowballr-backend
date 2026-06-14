package se.uulm.snowballr.backend.model.dto.paper

import kotlinx.serialization.Serializable

@Serializable
data class ExternalId(
    val type: ExternalIdType,
    val value: String,
)
