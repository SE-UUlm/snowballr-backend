package se.uulm.snowballr.backend.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.table.columntypes.obfuscatedJson
import java.time.OffsetDateTime

/**
 * Represents the database table "project" and provides a mapping for managing project-related entities in the database.
 *
 * Columns:
 * - [name]: Represents the name of the project as a [String].
 * - [status]: Represents the status of the project as an enumeration value from [ProjectStatus].
 * - [currentStage]: Represents the current stage of the project as a [Long].
 * - [maxStage]: Represents the maximum stage of the project as a [Long].
 * - [similarityThreshold]: Represents the similarity threshold of the project as a [Float].
 * - [snowballingType]: Represents the type of snowballing used by the project as an enumeration value from
 * [SnowballingType].
 * - [reviewMaybeAllowed]: Represents whether the project allows reviews with a [ReviewDecision.MAYBE] as a [Boolean].
 * - [reviewDecisionMatrixBinary]: Represents the decision matrix on how the [PaperDecision] for a paper should be
 * determined as a [ByteArray].
 * - [fetchers]: Represents the fetchers used by the project as a JSON object mapping the fetcher names to their
 * options.
 * - [currentStageStartedAt]: Represents the timestamp of when the current stage of the project was started as a
 * [OffsetDateTime].
 * - [createdAt]: Represents the timestamp of when the project was created as an [OffsetDateTime].
 * - [createdBy]: A foreign key referencing the user table, representing the user who created the project.
 * - [modifiedAt]: Represents the timestamp of when the project was last modified as an [OffsetDateTime].
 * - [modifiedBy]: A foreign key referencing the user table, representing the user who last modified the project.
 * - [deletedAt]: Represents the timestamp of when the project was deleted as an [OffsetDateTime].
 * - [deletedBy]: A nullable foreign key referencing the user table, representing the user who deleted the project.
 * - [archivedAt]: Represents the timestamp of when the project was archived as an [OffsetDateTime].
 * - [archivedBy]: A nullable foreign key referencing the user table, representing the user who archived the project.
 */
object ProjectTable : UUIDTable("project") {
    val name = text("name")
    val status = enumeration<ProjectStatus>("status")
    val currentStage = integer("current_stage")
    val maxStage = integer("max_stage")
    val similarityThreshold = float("similarity_threshold")
    val snowballingType = enumeration<SnowballingType>("snowballing_type")
    val reviewMaybeAllowed = bool("review_maybe_allowed")
    val reviewDecisionMatrixBinary = redactedBinary("review_decision_matrix")
    val fetchers = obfuscatedJson<FetcherMap>("fetchers", Json)
    val currentStageStartedAt = timestampWithTimeZone("current_stage_started_at").clientDefault { OffsetDateTime.now() }

    // Metadata

    val createdAt = createdAt()
    val createdBy = createdBy()
    val modifiedAt = modifiedAt()
    val modifiedBy = modifiedBy()
    val deletedAt = deletedAt()
    val deletedBy = deletedBy()
    val archivedAt = timestampWithTimeZone("archived_at").nullable()

    /**
     * Nullable reference to the user who archived the project.
     *
     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a project
     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
     */
    val archivedBy = userReference("archived_by", ReferenceOption.RESTRICT, ReferenceOption.CASCADE).nullable()
}

/**
 * Creates a [Project] from this [ResultRow].
 */
fun ResultRow.toProject() = Project(
    id = this[ProjectTable.id].value,
    name = this[ProjectTable.name],
    status = this[ProjectTable.status],
    currentStage = this[ProjectTable.currentStage],
    maxStage = this[ProjectTable.maxStage],
    settings = ProjectSettings(
        similarityThreshold = this[ProjectTable.similarityThreshold],
        snowballingType = this[ProjectTable.snowballingType],
        reviewMaybeAllowed = this[ProjectTable.reviewMaybeAllowed],
        reviewDecisionMatrix = ReviewDecisionMatrix.parseFrom(this[ProjectTable.reviewDecisionMatrixBinary]),
        fetchers = this[ProjectTable.fetchers],
    ),
    currentStageStartedAt = this[ProjectTable.currentStageStartedAt],
    createdAt = this[ProjectTable.createdAt],
    createdBy = this[ProjectTable.createdBy].value,
    modifiedAt = this[ProjectTable.modifiedAt],
    modifiedBy = this[ProjectTable.modifiedBy]?.value,
    deletedAt = this[ProjectTable.deletedAt],
    deletedBy = this[ProjectTable.deletedBy]?.value,
    archivedAt = this[ProjectTable.archivedAt],
    archivedBy = this[ProjectTable.archivedBy]?.value,
)
