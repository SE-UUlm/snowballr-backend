package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.CriterionTable
import snowballr.CriterionOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [CriterionTable].
 */
data class Criterion(
    val id: UUID,
    val tag: String,
    val name: String,
    val description: String,
    val category: CriterionOuterClass.CriterionCategory,
    val projectId: UUID,
    val createdAt: OffsetDateTime,
    val createdBy: UUID?,
)

/**
 * Creates a [CriterionOuterClass.Criterion] from this [Criterion].
 */
fun Criterion.toGrpcCriterion(): CriterionOuterClass.Criterion =
    CriterionOuterClass.Criterion
        .newBuilder()
        .setId(this.id.toString())
        .setTag(this.tag)
        .setName(this.name)
        .setDescription(this.description)
        .setCategory(this.category)
        .build()
