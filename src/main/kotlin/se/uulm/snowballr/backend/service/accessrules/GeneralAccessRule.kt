@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.dto.isServerAdmin
import javax.annotation.CheckReturnValue

/**
 * Check whether the requesting user is a server admin.
 */
@CheckReturnValue
fun isServerAdmin() = AccessRule<Unit> { requester, _ -> requester.isServerAdmin() }
