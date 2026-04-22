package se.uulm.snowballr.backend.access

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.UserIdentifierType
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus

class UserAccessCheckerTest {
    private val projectMemberRepo = mockk<IProjectMemberTableRepo>()

    private val accessChecker = UserAccessChecker(projectMemberRepo)

    @Nested
    inner class IsAllowedToReadUser {
        @Test
        fun `When the user is the same user and the target is active, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

            assertDoesNotThrow {
                accessChecker.isAllowedToReadUser(user, user, UserIdentifierType.ID)
            }
        }

        @Test
        fun `When the user is a server admin and the target is not active, then access is allowed`() = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val targetUser = DataBuilder.createExampleUser()

            assertDoesNotThrow {
                accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)
            }
        }

        @Test
        fun `When the user is in the same project as the target and the target is active, then access is allowed`() =
            runTest {
                val currentUser = DataBuilder.createExampleUser()
                val targetUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)
                val sharedMember = DataBuilder.createExampleProjectMember(userId = currentUser.id)

                coEvery { projectMemberRepo.getMembersInSameProjectsAsUser(targetUser.id) } returns listOf(sharedMember)

                assertDoesNotThrow {
                    accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)
                }
            }

        @Test
        fun `When the user is the same user but the target is not active, then a UserNotFoundException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()

                assertThrows<UserNotFoundException> {
                    accessChecker.isAllowedToReadUser(user, user, UserIdentifierType.ID)
                }
            }

        @Test
        fun `When identifierType is EMAIL and target is inactive, then a UserNotFoundByEmailException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()

                assertThrows<UserNotFoundByEmailException> {
                    accessChecker.isAllowedToReadUser(user, user, UserIdentifierType.EMAIL)
                }
            }

        @Test
        fun `When the user is in the same project but the target is not active, then a UserNotFoundException is thrown`() =
            runTest {
                val currentUser = DataBuilder.createExampleUser()
                val targetUser = DataBuilder.createExampleUser()
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
                val targetUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

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
        @Test
        fun `When the user is the same user, then access is allowed regardless of active status`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertDoesNotThrow { accessChecker.isAllowedToUpdateUser(user, user) }
        }

        @Test
        fun `When the user is a server admin and the target is active, then access is allowed`() = runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val targetUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

            assertDoesNotThrow { accessChecker.isAllowedToUpdateUser(currentUser, targetUser) }
        }

        @Test
        fun `When the user is a server admin but the target is not active, then an EntityNotActiveException is thrown`() =
            runTest {
                val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
                val targetUser = DataBuilder.createExampleUser()

                assertThrows<EntityNotActiveException> { accessChecker.isAllowedToUpdateUser(currentUser, targetUser) }
            }

        @Test
        fun `When the user is neither the same user nor a server admin, then access is denied`() = runTest {
            val currentUser = DataBuilder.createExampleUser()
            val targetUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

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
            val currentUser = DataBuilder.createExampleUser()
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
            val user = DataBuilder.createExampleUser()

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
            val targetUser = DataBuilder.createExampleUser()

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
            val currentUser = DataBuilder.createExampleUser()
            val targetUser = DataBuilder.createExampleUser()

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToDeleteUser(currentUser, targetUser) }
        }
    }
}
