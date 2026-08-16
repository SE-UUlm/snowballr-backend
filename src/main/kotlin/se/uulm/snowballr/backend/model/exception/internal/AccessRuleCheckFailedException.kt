package se.uulm.snowballr.backend.model.exception.internal

import se.uulm.snowballr.backend.model.exception.InternalException

/**
 * Represents an exception that occurs when an access rule chain completes with the final access check result of `false`
 * without throwing a specific exception.
 *
 * This indicates that the rule chain was built without covering all failure paths with explicit `orElseThrow(...)`
 * calls. In production, this exception should not be thrown and specific exceptions should be defined.
 *
 * Example cause:
 * ```
 * val rule = isAdmin.orElse(isSameUser)
 * rule.checkFor(currentUser, target)
 * ```
 * The check would return `false`, as the user is neither an admin nor the same user. In this case it should be
 * explicitly specified, which exception should be thrown.
 *
 * The recommended fix is to attach an explicit exception to each logical branch so that the user gets an appropriate
 * `UnauthorizedException` in the example.
 */
class AccessRuleCheckFailedException : InternalException(
    "Access rule check failed without throwing a specified exception. " +
        "Please add a more specific exception to each path in the rule chain.",
)
