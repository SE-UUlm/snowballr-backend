package se.uulm.snowballr.backend.model.incoming.criterion

import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import java.util.UUID

data class UpdateCriterionRequest(
    val criterionId: UUID,
    val tag: String,
    val name: String,
    val description: String,
    val category: CriterionCategory,
) {
    companion object {
        fun fromCriterion(criterion: Criterion) = UpdateCriterionRequest(
            criterionId = criterion.id,
            tag = criterion.tag,
            name = criterion.name,
            description = criterion.description,
            category = criterion.category,
        )
    }
}
