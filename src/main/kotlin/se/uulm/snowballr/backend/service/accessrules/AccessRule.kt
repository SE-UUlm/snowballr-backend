package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.AccessRuleCheckFailedException
import se.uulm.snowballr.backend.model.dto.User
import kotlin.reflect.KProperty1

/**
 * Represents a single access control rule from a user to an entity of type T
 * that can be evaluated in a chain.
 *
 * This interface is `suspend` as some access checks require asynchronous
 * operations, such as: fetching data from a database (e.g., project memberships).
 * By making every access rule `suspend`, we can easily chain rules that actually need to
 * suspend with rules that does not necessarily need to suspend without any checking, whether
 * one rule in the chain needs to suspend. Moreover, it no data need to be fetcher or similar tasks
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
 * Otherwise, the given exception is thrown and the chains terminates.
 *
 * @param T The accessed entity type.
 * @param exception Throw the [SnowballRException] if this rule does not hold.
 */
fun <T> AccessRule<T>.orElseThrow(exception: SnowballRException): AccessRule<T> =
    AccessRule { requester: User, target: T ->
        if (!this.isAllowedToAccess(requester, target)) {
            throw exception
        }
        true
    }

/**
 * Lifts a rule that ignores the target entity type (Unit) to work with any target entity type [T].
 *
 * @return An [AccessRule] for the target type [T] that applies the same logic.
 */
fun <T> AccessRule<Unit>.forTarget(): AccessRule<T> = AccessRule { requester, _ ->
    this.isAllowedToAccess(requester, Unit)
}

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
