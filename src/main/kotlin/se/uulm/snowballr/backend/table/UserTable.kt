package se.uulm.snowballr.backend.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.json.json
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPatternEntry
import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserSettings
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import java.time.OffsetDateTime
import java.util.UUID

fun patternOf(vararg decisions: Pair<ReviewDecision, Int>, result: PaperDecision) = DecisionMatrixPattern(
    decision = result,
    entries = decisions.map { (decision, count) -> DecisionMatrixPatternEntry(decision, count) },
)

private const val REQUIRED_REVIEWERS = 2
private val ACCEPT_DECLINE_PATTERN = patternOf(
    ReviewDecision.ACCEPTED to 1,
    ReviewDecision.DECLINED to 1,
    result = PaperDecision.IN_REVIEW,
)
private val ACCEPT_ANY_PATTERN = patternOf(
    ReviewDecision.ACCEPTED to 1,
    result = PaperDecision.ACCEPTED,
)
private val DECLINE_ANY_PATTERN = patternOf(
    ReviewDecision.DECLINED to 1,
    result = PaperDecision.DECLINED,
)
private val MAYBE_MAYBE_PATTERN = patternOf(
    ReviewDecision.MAYBE to REQUIRED_REVIEWERS,
    result = PaperDecision.IN_REVIEW,
)

private const val ARE_HOTKEYS_SHOWN_DEFAULT = true
private const val IS_REVIEW_MODE_ENABLED_DEFAULT = false
private val CRITERIA_IDS_DEFAULT = emptyList<UUID>()
private const val SIMILARITY_THRESHOLD_DEFAULT = 0.85F

/**
 * This default decision matrix assumes two reviewers by default.
 *
 * It encodes the basic rules for combining reviewer decisions.
 * Patterns are checked in order, and the first pattern whose entry count requirements
 * are satisfied determines the result:
 *  - Accept + Decline → still in review (need final decision)
 *  - Accept + Anything not already matched → Accepted
 *  - Decline + Anything not already matched → Declined
 *  - Maybe + Maybe → still in review (need final decision)
 */
private val DECISION_MATRIX_DEFAULT: ByteArray = ReviewDecisionMatrix(
    numberOfReviewers = REQUIRED_REVIEWERS,
    patterns = listOf(
        ACCEPT_DECLINE_PATTERN,
        ACCEPT_ANY_PATTERN,
        DECLINE_ANY_PATTERN,
        MAYBE_MAYBE_PATTERN,
    ),
).toByteArray()
private val FETCHERS_DEFAULT: FetcherMap = emptyMap()
private val SNOWBALLING_TYPE_DEFAULT = SnowballingType.BOTH
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
 * - [fetchers]: Represents the fetchers used by the project as a JSON object mapping the fetcher names to their
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
    val decisionMatrix = redactedBinary("review_decision_matrix").clientDefault { DECISION_MATRIX_DEFAULT }
    val fetchers = json<FetcherMap>("fetchers", Json).clientDefault { FETCHERS_DEFAULT }
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
    defaultProjectSettings = ProjectSettings(
        similarityThreshold = this[UserTable.similarityThreshold],
        reviewDecisionMatrix = ReviewDecisionMatrix.parseFrom(this[UserTable.decisionMatrix]),
        fetchers = this[UserTable.fetchers],
        snowballingType = this[UserTable.snowballingType],
        reviewMaybeAllowed = this[UserTable.reviewMaybeAllowed],
    ),
)
