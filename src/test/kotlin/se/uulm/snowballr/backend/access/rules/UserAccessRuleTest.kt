package se.uulm.snowballr.backend.access.rules

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.internal.AccessRuleCheckFailedException
import java.util.UUID

class UserAccessRuleTest {
    companion object {
        @JvmStatic
        fun activeStatuses(): List<UserStatus> = listOf(
            UserStatus.ACTIVE,
            UserStatus.ACTIVE_UNCONFIRMED,
        )

        @JvmStatic
        fun inactiveStatuses() = UserStatus.entries.filter { !activeStatuses().contains(it) }
    }

    @Nested
    inner class IsServerAdmin {
        @Test
        fun `When the requester is a server admin, then no exception is thrown`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.ADMIN)

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
            val user = DataBuilder.createExampleUser(role = UserRole.ADMIN)
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
        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.UserAccessRuleTest#activeStatuses")
        fun `When the target user is active, then no exception is thrown`(status: UserStatus) = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = status)

            assertDoesNotThrow { isTargetUserActive().checkFor(requester, target) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.UserAccessRuleTest#inactiveStatuses")
        fun `When the target user is inactive, then an AccessRuleCheckFailedException is thrown`(status: UserStatus) =
            runTest {
                val requester = DataBuilder.createExampleUser()
                val target = DataBuilder.createExampleUser(status = status)

                assertThrows<AccessRuleCheckFailedException> { isTargetUserActive().checkFor(requester, target) }
            }
    }

    @Nested
    inner class IsServerAdminOrTargetUserActive {
        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.UserAccessRuleTest#inactiveStatuses")
        fun `When the requester is a server admin and the target is inactive, then no exception is thrown`(
            status: UserStatus,
        ) = runTest {
            val requester = DataBuilder.createExampleUser(role = UserRole.ADMIN)
            val target = DataBuilder.createExampleUser(status = status)

            assertDoesNotThrow { isServerAdminOrTargetUserActive().checkFor(requester, target) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.UserAccessRuleTest#activeStatuses")
        fun `When the target user is active, then no exception is thrown`(status: UserStatus) = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = status)

            assertDoesNotThrow { isServerAdminOrTargetUserActive().checkFor(requester, target) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.UserAccessRuleTest#inactiveStatuses")
        fun `When the requester is not a server admin and the target is inactive, then an AccessRuleCheckFailedException is thrown`(
            status: UserStatus,
        ) = runTest {
            val requester = DataBuilder.createExampleUser()
            val target = DataBuilder.createExampleUser(status = status)

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
            val target = DataBuilder.createExampleUser(role = UserRole.ADMIN)

            assertThrows<AccessRuleCheckFailedException> { isTargetUserNotAdmin().checkFor(requester, target) }
        }
    }
}
