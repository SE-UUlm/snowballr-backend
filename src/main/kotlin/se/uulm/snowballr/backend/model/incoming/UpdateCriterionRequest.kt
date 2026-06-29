package se.uulm.snowballr.backend.model.incoming

import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import java.util.UUID

data class UpdateCriterionRequest(
    val criterionId: UUID,
    val tag: String,
    val name: String,
    val description: String,
    val category: CriterionCategory,
)
