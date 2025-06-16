package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.EnumerationColumnType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import se.uulm.snowballr.backend.model.FetcherApi
import se.uulm.snowballr.backend.model.dto.Project
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass
import java.time.OffsetDateTime

/**
 * Represents the database table "project" and provides a mapping for managing project-related entities in the database.
 *
 * Columns:
 * - [name]: Represents the name of the project as a [String].
 * - [status]: Represents the status of the project as an enumeration value from [ProjectOuterClass.ProjectStatus].
 * - [currentStage]: Represents the current stage of the project as a [Long].
 * - [maxStage]: Represents the maximum stage of the project as a [Long].
 * - [similarityThreshold]: Represents the similarity threshold of the project as a [Float].
 * - [snowballingType]: Represents the type of snowballing used by the project as an enumeration value from
 * [ProjectOuterClass.SnowballingType].
 * - [reviewMaybeAllowed]: Represents whether the project allows reviews with a
 * [ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE] as a [Boolean].
 * - [reviewDecisionMatrixBinary]: Represents the decision matrix on how the [ProjectOuterClass.PaperDecision] for a
 * paper should be determined as a [ByteArray].
 * - [fetcherApis]: Represents the fetcher APIs used by the project as a list of [FetcherApi] values.
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
object ProjectTable : IntIdTable("project") {
    val name = text("name")
    val status = enumeration<ProjectOuterClass.ProjectStatus>("status")
    val currentStage = long("current_stage")
    val maxStage = long("max_stage")
    val similarityThreshold = float("similarity_threshold")
    val snowballingType = enumeration<ProjectOuterClass.SnowballingType>("snowballing_type")
    val reviewMaybeAllowed = bool("review_maybe_allowed")
    val reviewDecisionMatrixBinary = binary("review_decision_matrix")
    val fetcherApis = array("fetcher_apis", EnumerationColumnType(FetcherApi::class))
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

    // Methods

    /**
     * Creates a [Project] from this [ResultRow].
     */
    fun ResultRow.toProject() =
        Project(
            id = this[id].value,
            name = this[name],
            status = this[status],
            currentStage = this[currentStage],
            maxStage = this[maxStage],
            similarityThreshold = this[similarityThreshold],
            snowballingType = this[snowballingType],
            reviewMaybeAllowed = this[reviewMaybeAllowed],
            reviewDecisionMatrix = ProjectOuterClass.ReviewDecisionMatrix.parseFrom(this[reviewDecisionMatrixBinary]),
            fetcherApis = this[fetcherApis],
            currentStageStartedAt = this[currentStageStartedAt],
            createdAt = this[createdAt],
            createdBy = this[createdBy].value,
            modifiedAt = this[modifiedAt],
            modifiedBy = this[modifiedBy]?.value,
            deletedAt = this[deletedAt],
            deletedBy = this[deletedBy]?.value,
            archivedAt = this[archivedAt],
            archivedBy = this[archivedBy]?.value,
        )
}
