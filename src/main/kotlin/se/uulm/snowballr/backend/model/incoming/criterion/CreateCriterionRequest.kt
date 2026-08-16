package se.uulm.snowballr.backend.model.incoming.criterion

import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import java.util.UUID

data class CreateCriterionRequest(
    val tag: String,
    val name: String,
    val description: String,
    val category: CriterionCategory,
    val projectId: UUID?,
)
