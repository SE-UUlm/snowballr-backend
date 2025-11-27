package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.exception.AccessRuleCheckFailedException
import se.uulm.snowballr.backend.model.exception.SnowballRException
import java.util.UUID
import kotlin.reflect.KProperty1

/**
 * Represents a single access control rule from a user to an entity of type T
 * that can be evaluated in a chain.
 *
 * This interface is `suspend` as some access checks require asynchronous
 * operations, such as fetching data from a database (e.g., project memberships).
 * By making every access rule `suspend`, we can easily chain rules that actually need to
 * suspend with rules that do not necessarily need to suspend without any checking, whether
 * one rule in the chain needs to suspend. Moreover, if no data need to be fetcher or similar tasks
 * to be performed, suspended functions are not significantly slower than the equal not suspended functions.
 */
fun interface AccessRule<T> {
    suspend fun isAllowedToAccess(requester: User, target: T): Boolean
}

/**
 * Checks whether this rule holds; if not, the fallback rule is checked.
 * This can be used for chaining rules with a logical OR.
 *
 * This method short-circuits: if the first rule passes, the fallback rule is not evaluated.
 *
 * @param T The accessed entity type.
 * @param fallback The fallback rule to check if this rule fails.
 * @return A new [AccessRule] that passes if either this rule or the fallback rule passes.
 */
fun <T> AccessRule<T>.orElse(fallback: AccessRule<T>): AccessRule<T> = AccessRule { requester: User, target: T ->
    this.isAllowedToAccess(requester, target) || fallback.isAllowedToAccess(requester, target)
}

/**
 * Checks whether this rule holds; if so, the next rule is checked.
 * This can be used for chaining rules with a logical AND.
 *
 * This method short-circuits: if the first rule fails, the next rule is not evaluated.
 *
 * @param T The accessed entity type.
 * @param next The next rule to check if this rule passes.
 * @return A new [AccessRule] that passes only if both this rule and the next rule pass.
 */
fun <T> AccessRule<T>.andAlso(next: AccessRule<T>): AccessRule<T> = AccessRule { requester: User, target: T ->
    this.isAllowedToAccess(requester, target) && next.isAllowedToAccess(requester, target)
}

/**
 * Checks whether this rule holds; if so, the next rule is checked.
 * Otherwise, the given exception is thrown and the chains terminate.
 *
 * @param T The accessed entity type.
 * @param exceptionProvider Creating the [SnowballRException] that is thrown if this rule does not hold.
 */
fun <T> AccessRule<T>.orElseThrow(exceptionProvider: (User, T) -> SnowballRException): AccessRule<T> =
    AccessRule { requester: User, target: T ->
        if (!this.isAllowedToAccess(requester, target)) {
            throw exceptionProvider(requester, target)
        }
        true
    }

/**
 * Checks whether this rule holds; if so, the next rule is checked.
 * Otherwise, the given exception is thrown and the chains terminate.
 *
 * @param T The accessed entity type.
 * @param exception The [SnowballRException] that is thrown if this rule does not hold.
 */
fun <T> AccessRule<T>.orElseThrow(exception: SnowballRException): AccessRule<T> =
    AccessRule { requester: User, target: T ->
        if (!this.isAllowedToAccess(requester, target)) {
            throw exception
        }
        true
    }

/**
 * Lifts a rule that ignores the target entity so that it can be applied to any target type [T].
 *
 * This is useful for rules that depend only on the requesting [User] and not on
 * the target entity. For example, "isServerAdmin" checks only the requester,
 * so it can be used regardless of what entity type is being accessed.
 *
 * Usage example:
 * ```
 * val ruleOnUnit: AccessRule<Unit> = isServerAdmin
 * val ruleOnUser: AccessRule<User> = adminRule.forTarget()
 * ```
 *
 * @return An [AccessRule] for the target type [T] that applies the same logic.
 */
fun <T> AccessRule<Unit>.forTarget(): AccessRule<T> = AccessRule { requester, _ ->
    this.isAllowedToAccess(requester, Unit)
}

/**
 * Adapts a rule defined on a property of type [TOld]
 * so that it can be applied to an entity of type [TNew] that contains
 * this property.
 *
 * This allows rules to be reused without redefining them for every entity type.
 * For example, if you already have a rule that checks access to a [UUID] (like
 * a `userId`), you can lift it to work on a whole [User] object by passing
 * `User::id` as the property reference.
 *
 * Usage example:
 * ```
 * val ruleOnId: AccessRule<UUID> = ...
 * val ruleOnUser: AccessRule<User> = ruleOnId.forProperty(User::id)
 * ```
 * @param TNew The target type of the adapted rule.
 * @param TOld The type of the property that the rule is defined on.
 * @param prop The property of [TNew] whose value should be passed into the original rule.
 * @return An [AccessRule] for target type [TNew] that delegates to this rule using the value of [prop].
 */
fun <TNew, TOld> AccessRule<TOld>.forProperty(prop: KProperty1<TNew, TOld>): AccessRule<TNew> =
    AccessRule { requester, newTarget -> this.isAllowedToAccess(requester, prop.get(newTarget)) }

/**
 * Executes the access rule against the given requesting user and target entity.
 *
 * If the rule chain returns `true`, the caller is authorized to proceed and no problem occurred.
 * If the rule chain returns `false` without having thrown an exception,
 * a [AccessRuleCheckFailedException] is thrown.
 *
 * @param T The accessed entity type.
 * @param requester The requesting user that wants to access the [target].
 * @param target The target entity that should be accessed.
 * @throws AccessRuleCheckFailedException if the rule chain evaluates to false
 *         without throwing a specific exception while evaluating the chain.
 */
suspend fun <T> AccessRule<T>.checkFor(requester: User, target: T) {
    val isAllowedToAccessEntity = this.isAllowedToAccess(requester, target)

    if (!isAllowedToAccessEntity) {
        throw AccessRuleCheckFailedException()
    }
}

/**
 * Executes the access rule for the given requesting user but **no** target entity.
 *
 * @param requester The requesting user that wants access.
 * @throws AccessRuleCheckFailedException if the rule chain evaluates to false
 *         without throwing a specific exception while evaluating the chain.
 */
suspend fun AccessRule<Unit>.checkFor(requester: User) = checkFor(requester, Unit)

/**
 * Represents a compound object containing two targets for access rule checks.
 *
 * This class is typically used in scenarios where access control requires validation involving
 * two related targets, such as ensuring permissions across multiple associated entities.
 *
 * @param T1 The type of the first target.
 * @param T2 The type of the second target.
 * @property firstTarget The first target.
 * @property secondTarget The second target.
 */
data class AccessRuleCompoundObject<T1, T2>(
    val firstTarget: T1,
    val secondTarget: T2,
)

typealias AccessRuleCompoundUUID = AccessRuleCompoundObject<UUID, UUID>
