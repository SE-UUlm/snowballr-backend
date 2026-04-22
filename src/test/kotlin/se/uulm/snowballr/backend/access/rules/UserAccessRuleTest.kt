package se.uulm.snowballr.backend.access.rules

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.internal.AccessRuleCheckFailedException
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

class UserAccessRuleTest {
    @Nested
    inner class IsServerAdmin {
        @Test
        fun `When the requester is a server admin, then no exception is thrown`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            assertDoesNotThrow { isServerAdmin().checkFor(user) }
        }

        @Test
        fun `When the requester is not a server admin, then an AccessRuleCheckFailedException is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertThrows<AccessRuleCheckFailedException> { isServerAdmin().checkFor(user) }
        }
    }

    @Nested
    inner class IsSameUserById {
        @Test
        fun `When the requester and target have the same ID, then no exception is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertDoesNotThrow { isSameUserById().checkFor(user, user.id) }
        }

        @Test
        fun `When the requester and target have different IDs, then an AccessRuleCheckFailedException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val otherUserId = UUID.randomUUID()

                assertThrows<AccessRuleCheckFailedException> { isSameUserById().checkFor(user, otherUserId) }
            }
    }

    @Nested
    inner class IsServerAdminOrSameUser {
        @Test
        fun `When the requester is a server admin, then no exception is thrown`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val otherUserId = UUID.randomUUID()

            assertDoesNotThrow { isServerAdminOrSameUser().checkFor(user, otherUserId) }
        }

        @Test
        fun `When the requester is the same user, then no exception is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertDoesNotThrow { isServerAdminOrSameUser().checkFor(user, user.id) }
        }

        @Test
        fun `When the requester is neither a server admin nor the same user, then an AccessRuleCheckFailedException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val otherUserId = UUID.randomUUID()

                assertThrows<AccessRuleCheckFailedException> { isServerAdminOrSameUser().checkFor(user, otherUserId) }
            }
    }

    @Nested
    inner class IsTargetUserActive {
        @Test
        fun `When the target user status is ACTIVE, then no exception is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

            assertDoesNotThrow { isTargetUserActive().checkFor(requester, target) }
        }

        @Test
        fun `When the target user status is ACTIVE_UNCONFIRMED, then no exception is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)

            assertDoesNotThrow { isTargetUserActive().checkFor(requester, target) }
        }

        @Test
        fun `When the target user is inactive, then an AccessRuleCheckFailedException is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_UNSPECIFIED)

            assertThrows<AccessRuleCheckFailedException> { isTargetUserActive().checkFor(requester, target) }
        }
    }

    @Nested
    inner class IsServerAdminOrTargetUserActive {
        @Test
        fun `When the requester is a server admin and the target is inactive, then no exception is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val target = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_UNSPECIFIED)

            assertDoesNotThrow { isServerAdminOrTargetUserActive().checkFor(requester, target) }
        }

        @Test
        fun `When the target user is active, then no exception is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

            assertDoesNotThrow { isServerAdminOrTargetUserActive().checkFor(requester, target) }
        }

        @Test
        fun `When the requester is not a server admin and the target is inactive, then an AccessRuleCheckFailedException is thrown`() =
            runTest {
                val requester = DataBuilder.createExampleUser()
                val target = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_UNSPECIFIED)

                assertThrows<AccessRuleCheckFailedException> {
                    isServerAdminOrTargetUserActive().checkFor(requester, target)
                }
            }
    }

    @Nested
    inner class IsTargetUserNotAdmin {
        @Test
        fun `When the target user is not a server admin, then no exception is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser()

            assertDoesNotThrow { isTargetUserNotAdmin().checkFor(requester, target) }
        }

        @Test
        fun `When the target user is a server admin, then an AccessRuleCheckFailedException is thrown`() = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            assertThrows<AccessRuleCheckFailedException> { isTargetUserNotAdmin().checkFor(requester, target) }
        }
    }
}
