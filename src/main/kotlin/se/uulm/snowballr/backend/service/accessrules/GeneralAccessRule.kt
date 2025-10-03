@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import snowballr.UserOuterClass.UserRole
import javax.annotation.CheckReturnValue

/**
 * Check whether the requesting user is a server admin.
 */
@CheckReturnValue
fun isServerAdmin() = AccessRule<Unit> { requester, _ -> requester.role == UserRole.USER_ROLE_ADMIN }
