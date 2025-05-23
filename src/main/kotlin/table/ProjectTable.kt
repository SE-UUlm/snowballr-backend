package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.EnumerationColumnType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import se.uulm.snowballr.backend.model.FetcherApi
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass
import java.time.OffsetDateTime

/**
 * Represents the database table "project" and provides a mapping for managing project-related entities in the database.
 *
 * Columns:
 * - [name]: Represents the name of the project as text.
 * - [status]: Represents the status of the project as an enumeration value from [ProjectOuterClass.ProjectStatus].
 * - [currentStage]: Represents the current stage of the project as a [Long].
 * - [maxStage]: Represents the maximum stage of the project as a [Long].
 * - [similarityThreshold]: Represents the similarity threshold of the project as a [Float].
 * - [snowballingType]: Represents the type of snowballing used by the project as an enumeration value from
 * [ProjectOuterClass.SnowballingType].
 * - [reviewMaybeAllowed]: Represents whether the project allows reviews with a
 * [ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE] as a [Boolean].
 * - [reviewDecisionMatrixBinary]: Represents the decision matrix on how the [ProjectOuterClass.PaperDecision] for a
 * paper should be determined as binary data.
 * - [fetcherApis]: Represents the fetcher APIs used by the project as a list of [FetcherApi] values.
 * - [createdAt]: Represents the timestamp of when the project was created as a [OffsetDateTime].
 * - [currentStageStartedAt]: Represents the timestamp of when the current stage of the project was started as a
 * [OffsetDateTime].
 * - [archivedAt]: Represents the timestamp of when the project was archived as a [OffsetDateTime].
 * - [deletedAt]: Represents the timestamp of when the project was deleted as a [OffsetDateTime].
 * - [archivedBy]: A nullable foreign key referencing the user table, representing the user who archived the project.
 * - [createdBy]: A foreign key referencing the user table, representing the user who created the project.
 * - [deletedBy]: A nullable foreign key referencing the user table, representing the user who deleted the project.
 */
object ProjectTable : IntIdTable("project") {
    val name = text("name")
    val status = enumeration("status", ProjectOuterClass.ProjectStatus::class)
    val currentStage = long("current_stage")
    val maxStage = long("max_stage")
    val similarityThreshold = float("similarity_threshold")
    val snowballingType = enumeration("snowballing_type", ProjectOuterClass.SnowballingType::class)
    val reviewMaybeAllowed = bool("review_maybe_allowed")
    val reviewDecisionMatrixBinary = binary("review_decision_matrix")
    val fetcherApis = array("fetcher_apis", EnumerationColumnType(FetcherApi::class))
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val currentStageStartedAt = timestampWithTimeZone("last_stage_started_at").clientDefault { OffsetDateTime.now() }
    val archivedAt = timestampWithTimeZone("archived_at").nullable()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

//    /**
//     * Reference to the user who created the project.
//     *
//     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a project
//     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
//     */
//    val createdBy = reference("create_by", UserTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

//    /**
//     * Nullable reference to the user who archived the project.
//     *
//     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a project
//     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
//     */
//    val archivedBy = reference("archived_by", UserTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE).nullable()

//    /**
//     * Nullable reference to the user who deleted the project.
//     *
//     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a project
//     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
//     */
//    val deletedBy = reference("deleted_by", UserTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE).nullable()

    /**
     * Creates a [ProjectOuterClass.Project] object from a database [ResultRow].
     */
    fun ResultRow.toProject(): ProjectOuterClass.Project {
        val settingsBuilder =
            ProjectOuterClass.Project.Settings
                .newBuilder()
                .setSimilarityThreshold(this[similarityThreshold])
                .setDecisionMatrix(ProjectOuterClass.ReviewDecisionMatrix.parseFrom(this[reviewDecisionMatrixBinary]))
                .setSnowballingType(this[snowballingType])
                .setReviewMaybeAllowed(this[reviewMaybeAllowed])
        for (api in this[fetcherApis]) {
            settingsBuilder.addFetcherApis(api.name)
        }
        val settings = settingsBuilder.build()

        return ProjectOuterClass.Project
            .newBuilder()
            .setId(this[id].value.toString())
            .setName(this[name])
            .setStatus(this[status])
            .setCurrentStage(this[currentStage])
            .setMaxStage(this[maxStage])
            .setSettings(settings)
            .build()
    }

    /**
     * Returns the entity ID of the project with the given [id] or `null` if no such project exists.
     * This can be used to reference the entity in other table rows.
     *
     * Example:
     * Table A stores a reference to [ProjectTable] as `project_id`. To create a row in table A, we can use this method
     * to get the [EntityID] and then pass it to the `project_id` column of table A.
     */
    fun ProjectTable.getEntityId(id: String): EntityID<Int>? =
        this
            .select(ProjectTable.id)
            .where { ProjectTable.id eq id.toInt() }
            .map { it[ProjectTable.id] }
            .singleOrNull()
}
