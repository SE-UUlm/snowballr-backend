package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.TooLongList
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.model.dto.user.UserField
import se.uulm.snowballr.backend.validation.ProjectSettingsValidator.validateDecisionMatrix
import se.uulm.snowballr.backend.validation.ProjectSettingsValidator.validateSimilarityThreshold
import se.uulm.snowballr.backend.validation.ProjectSettingsValidator.validateSnowballingType
import snowballr.UserOuterClass.User
import snowballr.UserSettingsOuterClass.UserSettings

/**
 * A validator for [User] related requests.
 *
 * @see AuthenticationValidator for more validation functions regarding the [User]
 */
object UserValidator {
    const val MAX_CRITERIA_COUNT = 50

    private const val FIELD_DEFAULT_CRITERIA = "user_settings.default_criteria"
    private const val FIELD_SNOWBALLING_TYPE = "user_settings.default_project_settings.snowballing_type"
    private const val FIELD_SIMILARITY_THRESHOLD = "user_settings.default_project_settings.similarity_threshold"
    private const val FIELD_DECISION_MATRIX = "user_settings.default_project_settings.decision_matrix"

    fun validateUpdateRequest(request: User.Update): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            val allowedPaths = listOf("user.id") + UserField.entries
                .filter { isValidUserUpdateField(it) }
                .map { getGrpcPathsForUserField(it) }
            ensureFieldMaskIsValid(request.mask, allowedPaths)
        }

        // If field mask validation fails, return early
        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        // Only proceed with field validation if the mask is valid
        val selectedFields = request.mask.pathsList.toSet()

        val user = request.user
        zipOrAccumulate(
            { ensureIdValidity("id", user.id) },
            {
                if ("user.email" in selectedFields) {
                    ensureEmailValidity(user.email)
                }
            },
            {
                if ("user.first_name" in selectedFields) {
                    ensureFirstNameValidity(user.firstName)
                }
            },
            {
                if ("user.last_name" in selectedFields) {
                    ensureLastNameValidity(user.lastName)
                }
            },
            {
                if ("user.role" in selectedFields) {
                    ensureEnumNotUnspecified("role", user.role)
                }
            },
        ) { _, _, _, _, _ -> }
    }

    fun validateUpdateSettingsRequest(request: UserSettings.Update): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            val allowedPaths = UserField.entries
                .filter { isValidUserSettingsUpdateField(it) }
                .map { getGrpcPathsForUserField(it) }
            ensureFieldMaskIsValid(request.mask, allowedPaths)
        }

        // If field mask validation fails, return early
        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        // Only proceed with field validation if the mask is valid
        val selectedFields = request.mask.pathsList.toSet()

        // ensureIdValidity("id", settings.id)

        val settings = request.userSettings
        val projectSettings = request.userSettings.defaultProjectSettings
        zipOrAccumulate(
            { validateSnowballingType(projectSettings, selectedFields, FIELD_SNOWBALLING_TYPE) },
            { validateSimilarityThreshold(projectSettings, selectedFields, FIELD_SIMILARITY_THRESHOLD) },
            { validateDecisionMatrix(projectSettings, selectedFields, FIELD_DECISION_MATRIX) },
            { validateCriterionList(settings, selectedFields) },
        ) { _, _, _, _ -> }
        validateCriteria(settings, selectedFields)
    }

    fun isValidUserUpdateField(field: UserField) = when (field) {
        UserField.EMAIL,
        UserField.FIRST_NAME,
        UserField.LAST_NAME,
        UserField.ROLE,
        -> true

        // unallowed non-settings field
        UserField.STATUS,
        // settings fields
        UserField.ARE_HOTKEYS_SHOWN,
        UserField.IS_REVIEW_MODE_ENABLED,
        UserField.CRITERIA_IDS,
        UserField.SIMILARITY_THRESHOLD,
        UserField.SNOWBALLING_TYPE,
        UserField.REVIEW_MAYBE_ALLOWED,
        UserField.FETCHERS,
        UserField.NUMBER_OF_REVIEWERS,
        UserField.DECISION_MATRIX_PATTERNS,
        -> false
    }

    fun isValidUserSettingsUpdateField(field: UserField) = when (field) {
        UserField.ARE_HOTKEYS_SHOWN,
        UserField.IS_REVIEW_MODE_ENABLED,
        UserField.CRITERIA_IDS,
        UserField.SIMILARITY_THRESHOLD,
        UserField.SNOWBALLING_TYPE,
        UserField.REVIEW_MAYBE_ALLOWED,
        UserField.FETCHERS,
        UserField.NUMBER_OF_REVIEWERS,
        UserField.DECISION_MATRIX_PATTERNS,
        -> true

        // non-settings fields
        UserField.EMAIL,
        UserField.FIRST_NAME,
        UserField.LAST_NAME,
        UserField.ROLE,
        UserField.STATUS,
        -> false
    }

    @Suppress("CyclomaticComplexMethod")
    fun getGrpcPathsForUserField(field: UserField) = when (field) {
        UserField.EMAIL -> "user.email"
        UserField.FIRST_NAME -> "user.first_name"
        UserField.LAST_NAME -> "user.last_name"
        UserField.ROLE -> "user.role"
        UserField.STATUS -> "user.status"
        UserField.ARE_HOTKEYS_SHOWN -> "user_settings.show_hotkeys"
        UserField.IS_REVIEW_MODE_ENABLED -> "user_settings.review_mode"
        UserField.CRITERIA_IDS -> FIELD_DEFAULT_CRITERIA
        UserField.SIMILARITY_THRESHOLD -> FIELD_SIMILARITY_THRESHOLD
        UserField.SNOWBALLING_TYPE -> FIELD_SNOWBALLING_TYPE
        UserField.REVIEW_MAYBE_ALLOWED -> "user_settings.default_project_settings.review_maybe_allowed"
        UserField.FETCHERS -> "user_settings.default_project_settings.fetchers"
        UserField.NUMBER_OF_REVIEWERS -> "user_settings.default_project_settings.decision_matrix.number_of_reviewers"
        UserField.DECISION_MATRIX_PATTERNS -> "user_settings.default_project_settings.decision_matrix.patterns"
    }

    private fun RaiseAccumulate<ValidationIssue>.validateCriterionList(
        settings: UserSettings,
        selectedFields: Set<String>,
    ) {
        if (FIELD_DEFAULT_CRITERIA in selectedFields) {
            ensure(settings.defaultCriteria.criteriaCount <= MAX_CRITERIA_COUNT) {
                TooLongList(FIELD_DEFAULT_CRITERIA, MAX_CRITERIA_COUNT)
            }
        }
    }

    private fun Raise<Nel<ValidationIssue>>.validateCriteria(settings: UserSettings, selectedFields: Set<String>) =
        validateElementList(
            FIELD_DEFAULT_CRITERIA,
            settings.defaultCriteria.criteriaList,
            CriterionValidator::validateCriterion,
            "criterion",
            selectedFields,
        )
}
