package se.uulm.snowballr.backend.access

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.UserIdentifierType
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo

class UserAccessCheckerTest {
    private val projectMemberRepo = mockk<IProjectMemberTableRepo>()

    private val accessChecker = UserAccessChecker(projectMemberRepo)

    companion object {
        @JvmStatic
        fun activeStatuses(): List<UserStatus> = listOf(
            UserStatus.USER_STATUS_ACTIVE,
            UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED,
        )

        @JvmStatic
        fun inactiveStatuses() = UserStatus.entries.filter { !activeStatuses().contains(it) }
    }

    @Nested
    inner class IsAllowedToReadUser {
        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#activeStatuses")
        fun `When the user is the same user and the target is active, then access is allowed`(status: UserStatus) =
            runTest {
                val user = DataBuilder.createExampleUser(status = status)

                assertDoesNotThrow {
                    accessChecker.isAllowedToReadUser(user, user, UserIdentifierType.ID)
                }
            }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#inactiveStatuses")
        fun `When the user is a server admin and the target is not active, then access is allowed`(status: UserStatus) =
            runTest {
                val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN, status = status)
                val targetUser = DataBuilder.createExampleUser()

                assertDoesNotThrow {
                    accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)
                }
            }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#activeStatuses")
        fun `When the user is in the same project as the target and the target is active, then access is allowed`(
            status: UserStatus,
        ) = runTest {
            val currentUser = DataBuilder.createExampleUser()
            val targetUser = DataBuilder.createExampleUser(status = status)
            val sharedMember = DataBuilder.createExampleProjectMember(userId = currentUser.id)

            coEvery { projectMemberRepo.getMembersInSameProjectsAsUser(targetUser.id) } returns listOf(sharedMember)

            assertDoesNotThrow {
                accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)
            }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#inactiveStatuses")
        fun `When the user is the same user but the target is not active, then a UserNotFoundException is thrown`(
            status: UserStatus,
        ) = runTest {
            val user = DataBuilder.createExampleUser(status = status)

            assertThrows<UserNotFoundException> {
                accessChecker.isAllowedToReadUser(user, user, UserIdentifierType.ID)
            }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#inactiveStatuses")
        fun `When identifierType is EMAIL and target is not active, then a UserNotFoundByEmailException is thrown`(
            status: UserStatus,
        ) = runTest {
            val user = DataBuilder.createExampleUser(status = status)

            assertThrows<UserNotFoundByEmailException> {
                accessChecker.isAllowedToReadUser(user, user, UserIdentifierType.EMAIL)
            }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#inactiveStatuses")
        fun `When the user is in the same project but the target is not active, then a UserNotFoundException is thrown`(
            status: UserStatus,
        ) = runTest {
            val currentUser = DataBuilder.createExampleUser()
            val targetUser = DataBuilder.createExampleUser(status = status)
            val sharedMember = DataBuilder.createExampleProjectMember(userId = currentUser.id)

            coEvery { projectMemberRepo.getMembersInSameProjectsAsUser(targetUser.id) } returns listOf(sharedMember)

            assertThrows<UserNotFoundException> {
                accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)
            }
        }

        @Test
        fun `When the user is not the same user, not a server admin, and not in the same project, then access is denied`() =
            runTest {
                val currentUser = DataBuilder.createExampleUser()
                val targetUser = DataBuilder.createExampleUser()

                coEvery { projectMemberRepo.getMembersInSameProjectsAsUser(targetUser.id) } returns emptyList()

                assertThrows<UnauthorizedReadException> {
                    accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)
                }
            }
    }

    @Nested
    inner class IsAllowedToReadAllUsers {
        @Test
        fun `When the user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            assertDoesNotThrow { accessChecker.isAllowedToReadAllUsers(user) }
        }

        @Test
        fun `When the user is not a server admin, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertThrows<UnauthorizedReadAllException> { accessChecker.isAllowedToReadAllUsers(user) }
        }
    }

    @Nested
    inner class IsAllowedToUpdateUser {
        @ParameterizedTest
        @EnumSource(UserStatus::class)
        fun `When the user is the same user, then access is allowed regardless of status`(status: UserStatus) =
            runTest {
                val user = DataBuilder.createExampleUser(status = status)

                assertDoesNotThrow { accessChecker.isAllowedToUpdateUser(user, user) }
            }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#activeStatuses")
        fun `When the user is a server admin and the target is active, then access is allowed`(status: UserStatus) =
            runTest {
                val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
                val targetUser = DataBuilder.createExampleUser(status = status)

                assertDoesNotThrow { accessChecker.isAllowedToUpdateUser(currentUser, targetUser) }
            }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.UserAccessCheckerTest#inactiveStatuses")
        fun `When the user is a server admin but the target is not active, then an EntityNotActiveException is thrown`(
            status: UserStatus,
        ) = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val targetUser = DataBuilder.createExampleUser(status = status)

            assertThrows<EntityNotActiveException> { accessChecker.isAllowedToUpdateUser(currentUser, targetUser) }
        }

        @Test
        fun `When the user is neither the same user nor a server admin, then access is denied`() = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val targetUser = DataBuilder.createExampleUser()

            assertThrows<UnauthorizedUpdateException> { accessChecker.isAllowedToUpdateUser(currentUser, targetUser) }
        }
    }

    @Nested
    inner class IsAllowedToUpdateUserRole {
        @Test
        fun `When the user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val targetUserId = DataBuilder.createExampleUser().id

            assertDoesNotThrow { accessChecker.isAllowedToUpdateUserRole(user, targetUserId) }
        }

        @Test
        fun `When the user is not a server admin, then access is denied`() = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val targetUserId = DataBuilder.createExampleUser().id

            assertThrows<UnauthorizedUpdateException> {
                accessChecker.isAllowedToUpdateUserRole(currentUser, targetUserId)
            }
        }
    }

    @Nested
    inner class IsAllowedToDeleteUser {
        @Test
        fun `When the user deletes themselves and is not an admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            assertDoesNotThrow { accessChecker.isAllowedToDeleteUser(user, user) }
        }

        @Test
        fun `When the user deletes themselves and is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            assertDoesNotThrow { accessChecker.isAllowedToDeleteUser(user, user) }
        }

        @Test
        fun `When a server admin deletes a non-admin user, then access is allowed`() = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val targetUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            assertDoesNotThrow { accessChecker.isAllowedToDeleteUser(currentUser, targetUser) }
        }

        @Test
        fun `When a server admin tries to delete another server admin, then a FailedPreconditionException is thrown`() =
            runTest {
                val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
                val targetUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

                assertThrows<FailedPreconditionException> {
                    accessChecker.isAllowedToDeleteUser(currentUser, targetUser)
                }
            }

        @Test
        fun `When the user is neither the same user nor a server admin, then access is denied`() = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val targetUser = DataBuilder.createExampleUser()

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToDeleteUser(currentUser, targetUser) }
        }
    }
}
