package se.uulm.snowballr.backend.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.json.json
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.UserSettings
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import java.util.UUID

private const val REQUIRED_REVIEWERS = 2

private const val ARE_HOTKEYS_SHOWN_DEFAULT = true
private const val IS_REVIEW_MODE_ENABLED_DEFAULT = false
private val CRITERIA_IDS_DEFAULT = emptyList<UUID>()
private const val SIMILARITY_THRESHOLD_DEFAULT = 0F
private val DECISION_MATRIX_DEFAULT: ByteArray = ReviewDecisionMatrix.newBuilder()
    .setNumberOfReviewers(REQUIRED_REVIEWERS)
    .build()
    .toByteArray()
private val FETCHERS_DEFAULT = emptyMap<String, Map<String, String>>()
private val SNOWBALLING_TYPE_DEFAULT = SnowballingType.SNOWBALLING_TYPE_BOTH
private const val REVIEW_MAYBE_ALLOWED_DEFAULT = true

/**
 * Represents the database table "user" and provides a mapping for managing user-related entities in the database.
 *
 * Columns:
 * - [email]: Represents the email address of the user as a [String].
 * - [firstName]: Represents the first name of the user as a [String].
 * - [lastName]: Represents the last name of the user as a [String].
 * - [passwordHash]: Represents the hashed password of the user as a [String].
 * - [role]: Represents the role of the user as an enumeration value from [UserRole].
 * - [status]: Represents the status of the user as an enumeration value from [UserStatus].
 * - [areHotkeysShown]: Represents whether the user has hotkeys displayed as a [Boolean].
 * - [reviewModeEnabled]: Represents whether the user is in review mode as a [Boolean].
 * - [criteriaIds]: Represents a list of criteria IDs associated with the user as a [List] of [UUID].
 * - [similarityThreshold]: Represents the user's similarity threshold as a [Float].
 * - [decisionMatrix]: Represents the review decision matrix of the user as a binary value.
 * - [fetchers]: Represents the fetchers used by the project as a json object mapping the fetcher names to their
 * options.
 * - [snowballingType]: Represents the snowballing type associated with the user, stored as an enumeration value of
 * [SnowballingType].
 * - [reviewMaybeAllowed]: Indicates whether "maybe" is allowed in reviews, stored as a [Boolean].
 * - [createdAt]: Represents the timestamp of when the user was created as an [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the user was last modified as an [OffsetDateTime].
 * - [deletedAt]: Represents the timestamp of when the user was last deleted as an [OffsetDateTime].
 */
object UserTable : UUIDTable("user") {
    val email = text("email").uniqueIndex()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val passwordHash = obfuscatedText("password_hash")
    val role = enumeration<UserRole>("role")
    val status = enumeration<UserStatus>("status")

    // User settings
    val areHotkeysShown = bool("show_hotkeys").clientDefault { ARE_HOTKEYS_SHOWN_DEFAULT }
    val reviewModeEnabled = bool("review_mode").clientDefault { IS_REVIEW_MODE_ENABLED_DEFAULT }
    val criteriaIds: Column<List<UUID>> = array<UUID>("criteria_ids").clientDefault { CRITERIA_IDS_DEFAULT }

    // Project settings defaults
    val similarityThreshold = float("similarity_threshold").clientDefault { SIMILARITY_THRESHOLD_DEFAULT }
    val decisionMatrix = binary("review_decision_matrix").clientDefault { DECISION_MATRIX_DEFAULT }
    val fetchers = json<Map<String, Map<String, String>>>("fetchers", Json).clientDefault { FETCHERS_DEFAULT }
    val snowballingType =
        enumeration<SnowballingType>("snowballing_type").clientDefault { SNOWBALLING_TYPE_DEFAULT }
    val reviewMaybeAllowed = bool("review_maybe_allowed").clientDefault { REVIEW_MAYBE_ALLOWED_DEFAULT }

    // Metadata
    val createdAt = createdAt()
    val modifiedAt = modifiedAt()
    val deletedAt = deletedAt()
}

/**
 * Creates a [User] from this [ResultRow].
 */
fun ResultRow.toUser() = User(
    id = this[UserTable.id].value,
    email = this[UserTable.email],
    firstName = this[UserTable.firstName],
    lastName = this[UserTable.lastName],
    role = this[UserTable.role],
    status = this[UserTable.status],
    createdAt = this[UserTable.createdAt],
    modifiedAt = this[UserTable.modifiedAt],
    deletedAt = this[UserTable.deletedAt],
)

/**
 * Creates [UserSettings] from this [ResultRow].
 */
fun ResultRow.toUserSettings() = UserSettings(
    areHotkeysShown = this[UserTable.areHotkeysShown],
    isReviewModeEnabled = this[UserTable.reviewModeEnabled],
    criteriaIds = this[UserTable.criteriaIds],
    similarityThreshold = this[UserTable.similarityThreshold],
    decisionMatrix = ReviewDecisionMatrix.parseFrom(this[UserTable.decisionMatrix]),
    fetchers = this[UserTable.fetchers],
    snowballingType = this[UserTable.snowballingType],
    reviewMaybeAllowed = this[UserTable.reviewMaybeAllowed],
)
