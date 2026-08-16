package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project.Member
import snowballr.ProjectOuterClass.Project.Member.Remove

private const val FIELD_PROJECT_ID = "project_id"
private const val FIELD_USER_ID = "user_id"
private const val FIELD_TOKEN = "token"
private const val FIELD_NEW_ROLE = "new_role"

/**
 * A validator for [Member] related requests.
 */
object ProjectMemberValidator {
    fun validateRemoveRequest(request: Remove): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureIdValidity(FIELD_PROJECT_ID, request.projectId) },
            { ensureEmailValidity(request.userEmail) },
        ) { _, _ -> }
    }

    fun validateInviteRequest(request: Member.Invite): EitherNel<ValidationIssue, Unit> = either {
        ensureFieldNonBlank(FIELD_PROJECT_ID, request.projectId)
        ensureIdValidity(FIELD_PROJECT_ID, request.projectId)
        ensureEmailValidity(request.userEmail)
    }.toEitherNel()

    fun validateAcceptRequest(request: Member.Accept): EitherNel<ValidationIssue, Unit> = either {
        ensureFieldNonBlank(FIELD_TOKEN, request.token)
    }.toEitherNel()

    fun validateMemberUpdateRequest(request: Member.Update): EitherNel<ValidationIssue, Unit> = either {
        ensureIdValidity(FIELD_PROJECT_ID, request.projectId)
        ensureIdValidity(FIELD_USER_ID, request.userId)
        ensureEnumNotUnspecified(FIELD_NEW_ROLE, request.newRole)
    }.toEitherNel()
}
