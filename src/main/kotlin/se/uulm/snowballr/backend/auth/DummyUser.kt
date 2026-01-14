package se.uulm.snowballr.backend.auth

import se.uulm.snowballr.backend.fetcher.FetcherMap
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.util.UUID

/**
 * A dummy user object that is used for testing purposes.
 *
 * This object contains hardcoded values for a user that can be used in tests or development
 * where a valid user is required.
 *
 * It is not intended for production use and should be replaced with actual user data in real applications.
 */
object DummyUser {
    var id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    var email: String = "alice.smith@example.com"
    var firstName: String = "Alice"
    var lastName: String = "Smith"
    var password: String = "VALIDPassword__1234"
    var passwordHash: String = PasswordUtils.hashPassword(password)
    var role: UserOuterClass.UserRole = UserOuterClass.UserRole.USER_ROLE_ADMIN
    var status: UserOuterClass.UserStatus = UserOuterClass.UserStatus.USER_STATUS_ACTIVE

    // user settings
    var areHotkeysShown: Boolean = true
    var isReviewModeEnabled: Boolean = false
    var criteriaIds: List<UUID> = emptyList()
    var similarityThreshold: Float = 0F
    var decisionMatrix: ByteArray = ProjectOuterClass.ReviewDecisionMatrix.getDefaultInstance().toByteArray()
    var fetchers: FetcherMap = emptyMap()
    var snowballingType: ProjectOuterClass.SnowballingType = ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH
    var reviewMaybeAllowed: Boolean = true
}
