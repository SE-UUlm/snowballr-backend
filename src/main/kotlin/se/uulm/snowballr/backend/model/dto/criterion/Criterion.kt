package se.uulm.snowballr.backend.model.dto.criterion

import se.uulm.snowballr.backend.table.CriterionTable
import snowballr.CriterionOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [CriterionTable].
 */
sealed interface Criterion {
    val id: UUID
    val tag: String
    val name: String
    val description: String
    val category: CriterionCategory
    val createdAt: OffsetDateTime
    val createdBy: UUID

    /**
     * [Criterion] that is owned by the project.
     */
    data class ProjectCriterion(
        override val id: UUID,
        override val tag: String,
        override val name: String,
        override val description: String,
        override val category: CriterionCategory,
        override val createdAt: OffsetDateTime,
        override val createdBy: UUID,
        val projectId: UUID,
    ) : Criterion

    /**
     * [Criterion] that is owned by the user.
     */
    data class UserCriterion(
        override val id: UUID,
        override val tag: String,
        override val name: String,
        override val description: String,
        override val category: CriterionCategory,
        override val createdAt: OffsetDateTime,
        override val createdBy: UUID,
    ) : Criterion
}

/**
 * Creates a [CriterionOuterClass.Criterion] from this [Criterion].
 */
fun Criterion.toGrpcCriterion(): CriterionOuterClass.Criterion = CriterionOuterClass.Criterion
    .newBuilder()
    .setId(this.id.toString())
    .setTag(this.tag)
    .setName(this.name)
    .setDescription(this.description)
    .setCategory(this.category.toGrpc())
    .build()

/**
 * Creates a list of [CriterionOuterClass.Criterion]s from this list of [Criterion]s.
 */
fun List<Criterion>.toGrpcCriteria(): CriterionOuterClass.Criterion.List {
    val builder = CriterionOuterClass.Criterion.List.newBuilder()
    this.forEach { builder.addCriteria(it.toGrpcCriterion()) }
    return builder.build()
}
