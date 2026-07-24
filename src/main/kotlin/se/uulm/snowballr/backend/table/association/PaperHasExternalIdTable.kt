package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.table.PaperTable

/**
 * Represents the database table "paper_has_external_id", storing typed external identifiers for papers.
 *
 * Columns:
 * - [paperId]: Foreign key referencing the [PaperTable] that owns this external ID.
 * - [type]: The type of the external identifier as an [ExternalIdType].
 * - [value]: The external identifier value as a [String].
 *
 * Constraints:
 * - Primary key on ([paperId], [type]): a paper can have at most one external ID per type.
 * - Unique index on ([type], [value]): a given type-value pair can only belong to one paper globally.
 */
object PaperHasExternalIdTable : Table("paper_has_external_id") {
    /**
     * Reference to the associated paper.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the paper is deleted
     * - `onUpdate=CASCADE` so that when the paper ID is updated, the foreign key ID is updated too
     */
    val paperId = reference("paper_id", PaperTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    val type = enumeration<ExternalIdType>("type")

    val value = text("value")

    override val primaryKey = PrimaryKey(paperId, type)

    init {
        uniqueIndex(type, value)
    }
}

/**
 * Creates an [ExternalId] from this [ResultRow].
 */
fun ResultRow.toExternalId() = ExternalId(
    type = this[PaperHasExternalIdTable.type],
    value = this[PaperHasExternalIdTable.value],
)

/**
 * Creates a [Pair] from this [ResultRow].
 */
fun ResultRow.toExternalIdPair() = Pair(
    this[PaperHasExternalIdTable.paperId].value,
    this.toExternalId(),
)
