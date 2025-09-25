@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import snowballr.UserOuterClass.UserRole
import java.util.UUID

/**
 * Represents an [AccessRule] to an unknown entity type.
 */
fun interface GeneralAccessRule : AccessRule<Unit>

/**
 * Represents an [AccessRule] to a user only identified by its UUID.
 */
fun interface UUIDAccessRule : AccessRule<UUID>

/**
 * Check whether the requesting user is a server admin.
 */
val isServerAdmin = GeneralAccessRule { requester, _ -> requester.role == UserRole.USER_ROLE_ADMIN }
