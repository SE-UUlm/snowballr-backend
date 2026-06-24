package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.isBetweenWithDelta
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.Authentication
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.UserOuterClass.User
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID

class UserTableRepoTest : RepositoryTest(arrayOf(UserTable, CriterionTable, ProjectTable)) {
    private val repo = UserTableRepo(db)
    private val criterionTableRepo = CriterionTableRepo(db)

    private val defaultThresholdDate = OffsetDateTime.now().minusDays(30)

    companion object {
        @JvmStatic
        fun validFieldMasks(): List<Arguments> = listOf(
            Arguments.of(listOf("user.email")),
            Arguments.of(listOf("user.first_name", "user.last_name")),
            Arguments.of(listOf("user.role")),
            Arguments.of(listOf("user.status")),
            Arguments.of(listOf("user.status", "user.role")),
        )
    }

    @Nested
    inner class GetUserById {
        @Test
        fun `When a user is found by their ID, then a successful result with the correct user is returned`() = runTest {
            val userId = insertUserAndGetId()
            val result = repo.getUserById(userId)

            val user = assertResultSuccess(result)
            assertEquals(userId, user.id)
            assertEquals("test.user@example.com", user.email)
            assertEquals("Test", user.firstName)
            assertEquals("User", user.lastName)
            assertEquals(UserRole.USER_ROLE_DEFAULT, user.role)
            assertEquals(UserStatus.USER_STATUS_ACTIVE, user.status)
        }

        @Test
        fun `When a user is not found by their ID, then a failed result with a NotFoundException is returned`() =
            runTest {
                val result = repo.getUserById(UUID.randomUUID())

                assertResultFailure<NotFoundException>(result)
            }
    }

    @Nested
    inner class GetUserByEmail {
        @Test
        fun `When a user is found by their email, then a successful result with the correct user is returned`() =
            runTest {
                val userId = insertUserAndGetId()
                val result = repo.getUserByEmail("test.user@example.com")

                val user = assertResultSuccess(result)
                assertEquals(userId, user.id)
                assertEquals("test.user@example.com", user.email)
                assertEquals("Test", user.firstName)
                assertEquals("User", user.lastName)
                assertEquals(UserRole.USER_ROLE_DEFAULT, user.role)
                assertEquals(UserStatus.USER_STATUS_ACTIVE, user.status)
            }

        @Test
        fun `When a user is not found by their email, then a failed result with a NotFoundException is returned`() =
            runTest {
                val result = repo.getUserByEmail("nonexistent email")

                assertResultFailure<NotFoundException>(result)
            }
    }

    @Nested
    inner class DoesUserExitsById {
        @Test
        fun `When a user with the given id exists, then true is returned`() = runTest {
            val userId = insertUserAndGetId()

            assertTrue(repo.doesUserExistById(userId))
        }

        @Test
        fun `When a user with the given id does not exist, then false is returned`() = runTest {
            assertFalse(repo.doesUserExistByEmail(UUID.randomUUID().toString()))
        }
    }

    @Nested
    inner class DoesUserExistByEmail {
        @Test
        fun `When a user with the given email exists, then true is returned`() = runTest {
            insertUserAndGetId()

            assertTrue(repo.doesUserExistByEmail("test.user@example.com"))
        }

        @Test
        fun `When a user with the given email does not exist, then false is returned`() = runTest {
            assertFalse(repo.doesUserExistByEmail("test.user@example.com"))
        }
    }

    @Nested
    inner class GetAllUsers {
        @Test
        fun `When users are found, then all users are returned`() = runTest {
            val userId1 = insertUserAndGetId(email = "test.user1@example.com", lastName = "User 2")
            val userId2 = insertUserAndGetId(email = "test.user2@example.com", lastName = "User 1")

            val users = repo.getAllUsers()

            assertThat(users).hasSize(2)
            val firstUser = users.find { it.id == userId1 }
            assertNotNull(firstUser)
            val secondUser = users.find { it.id == userId2 }
            assertNotNull(secondUser)
        }

        @Test
        fun `When permanently deleted users exist, then only the existent users are returned`() = runTest {
            val userId1 = insertUserAndGetId(email = "test.user1@example.com", lastName = "User 1")
            val userId2 = insertUserAndGetId(email = "", firstName = "", lastName = "")

            val users = repo.getAllUsers()

            assertThat(users).hasSize(1)
            val firstUser = users.find { it.id == userId1 }
            assertNotNull(firstUser)
            val secondUser = users.find { it.id == userId2 }
            assertNull(secondUser)
        }
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `When a user is created, then the user is returned`() = runTest {
            val request =
                Authentication.RegisterRequest
                    .newBuilder()
                    .setEmail("alice.smith@example.com")
                    .setFirstName("Alice")
                    .setLastName("Smith")
                    .setPassword("AAbb__00")
                    .build()

            val user = repo.createUser(request, "hashedPassword")

            assertEquals("alice.smith@example.com", user.email)
            assertEquals("Alice", user.firstName)
            assertEquals("Smith", user.lastName)
            assertEquals(UserRole.USER_ROLE_DEFAULT, user.role)
            assertEquals(UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED, user.status)
        }

        @Test
        fun `When a user with an existent email is created, then an SQLException is thrown`() = runTest {
            val request =
                Authentication.RegisterRequest
                    .newBuilder()
                    .setEmail("alice.smith@example.com")
                    .setFirstName("Alice")
                    .setLastName("Smith")
                    .setPassword("AAbb__00")
                    .build()

            repo.createUser(request, "hashedPassword")

            assertThrows<SQLException> {
                repo.createUser(request, "hashedPassword2")
            }
        }

        @Test
        fun `When two users with different emails are created, then they have different IDs`() = runTest {
            val request1 =
                Authentication.RegisterRequest
                    .newBuilder()
                    .setEmail("alice.smith@example.com")
                    .setFirstName("Alice")
                    .setLastName("Smith")
                    .setPassword("AAbb__00")
                    .build()

            val user1 = repo.createUser(request1, "hashedPassword1")

            val request2 =
                Authentication.RegisterRequest
                    .newBuilder()
                    .setEmail("john.smith@example.com")
                    .setFirstName("John")
                    .setLastName("Smith")
                    .setPassword("BBaa__00")
                    .build()

            val user2 = repo.createUser(request2, "hashedPassword2")

            assertNotEquals(user2.id, user1.id)
        }
    }

    @Nested
    inner class UpdateUser {
        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.UserTableRepoTest#validFieldMasks")
        fun `When a user is updated, then only the fields specified in the field mask are updated and the updated user is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val userId = insertUserAndGetId(email = "test.user@example.com")
            val originalUser = repo.getUserById(userId).getOrThrow()

            val updatedUserDetails = originalUser.toGrpcUser().toBuilder()
                .setEmail("updated.user@example.com")
                .setFirstName("John")
                .setLastName("Doe")
                .setRole(UserRole.USER_ROLE_ADMIN)
                .setStatus(UserStatus.USER_STATUS_DELETED)
                .build()

            val request = User.Update.newBuilder()
                .setUser(updatedUserDetails)
                .setMask(FieldMaskUtil.fromStringList(fieldMask))
                .build()

            val updatedUser = repo.updateUser(request)

            if ("user.email" in fieldMask) {
                assertEquals("updated.user@example.com", updatedUser.email)
            } else {
                assertEquals("test.user@example.com", updatedUser.email)
            }
            if ("user.first_name" in fieldMask) {
                assertEquals("John", updatedUser.firstName)
            } else {
                assertEquals("Test", updatedUser.firstName)
            }
            if ("user.last_name" in fieldMask) {
                assertEquals("Doe", updatedUser.lastName)
            } else {
                assertEquals("User", updatedUser.lastName)
            }
            if ("user.role" in fieldMask) {
                assertEquals(UserRole.USER_ROLE_ADMIN, updatedUser.role)
            } else {
                assertEquals(UserRole.USER_ROLE_DEFAULT, updatedUser.role)
            }
            if ("user.status" in fieldMask) {
                assertEquals(UserStatus.USER_STATUS_DELETED, updatedUser.status)
            } else {
                assertEquals(UserStatus.USER_STATUS_ACTIVE, updatedUser.status)
            }
        }

        @Test
        fun `When a user's email should be updated to an existent email, then an SQLException is thrown`() = runTest {
            insertUserAndGetId(email = "alice.smith@example.com")

            val user2Id = insertUserAndGetId(email = "bob.smith@example.com")
            val user2Builder = repo.getUserById(user2Id).getOrThrow().toGrpcUser().toBuilder()

            val updateRequest =
                User.Update
                    .newBuilder()
                    .setUser(user2Builder.setEmail("alice.smith@example.com").build())
                    .setMask(FieldMaskUtil.fromString("user.email"))
                    .build()

            assertThrows<SQLException> {
                repo.updateUser(updateRequest)
            }
        }
    }

    @Nested
    inner class SoftDeleteUser {
        @Test
        fun `When the user is found, then the status of the user is set to USER_STATUS_DELETED`() = runTest {
            val userId1 = insertUserAndGetId()
            val before = OffsetDateTime.now()

            repo.softDeleteUser(userId1)

            val after = OffsetDateTime.now()
            val deletedUser = repo.getUserById(userId1).getOrThrow()

            assertEquals(UserStatus.USER_STATUS_DELETED, deletedUser.status)
            assertThat(deletedUser.deletedAt).isBetweenWithDelta(before, after)
        }

        @Test
        fun `When a user is not found, then no exception is thrown`() = runTest {
            assertDoesNotThrow { repo.softDeleteUser(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class ClearSoftDeletedUsers {
        @Test
        fun `When no soft-deleted users exist, then no users are cleared`() = runTest {
            val userId = insertUserAndGetId(status = UserStatus.USER_STATUS_ACTIVE)

            repo.clearSoftDeletedUsers(defaultThresholdDate)

            val user = assertResultSuccess(repo.getUserById(userId))
            assertEquals(UserStatus.USER_STATUS_ACTIVE, user.status)
            assertNull(user.deletedAt)
        }

        @Test
        fun `When soft-deleted users exist but their threshold date is not reached, then no users are cleared`() =
            runTest {
                val userId = insertUserAndGetId(status = UserStatus.USER_STATUS_ACTIVE)
                repo.softDeleteUser(userId)

                repo.clearSoftDeletedUsers(defaultThresholdDate)

                val user = assertResultSuccess(repo.getUserById(userId))
                assertEquals(UserStatus.USER_STATUS_DELETED, user.status)
                assertNotNull(user.deletedAt)
                assertThat(user.deletedAt).isAfter(defaultThresholdDate)
                assertThat(user.firstName).isNotEmpty()
            }

        @Test
        fun `When soft-deleted users exist and their threshold date is reached, then all soft-deleted users but their user-criteria are not cleared`() =
            runTest {
                // Manually "soft-delete" user to set the `deletedAt` date
                val userId1 = insertUserAndGetId(
                    email = "user1@test.de",
                    status = UserStatus.USER_STATUS_DELETED,
                    deletedAt = defaultThresholdDate.minusDays(1),
                )
                val userId2 = insertUserAndGetId(email = "user2@test.de", status = UserStatus.USER_STATUS_ACTIVE)
                val criteria1 = RepositoryHelper.insertCriterionAndGetId(createdBy = userId1)
                val criteria2 = RepositoryHelper.insertCriterionAndGetId(createdBy = userId2)

                repo.clearSoftDeletedUsers(defaultThresholdDate)

                val user1 = assertResultSuccess(repo.getUserById(userId1))
                assertEquals(UserStatus.USER_STATUS_UNSPECIFIED, user1.status)
                assertNotNull(user1.deletedAt)
                assertThat(user1.deletedAt).isBefore(defaultThresholdDate)
                assertThat(user1.firstName).isEmpty()
                assertResultSuccess(criterionTableRepo.getCriterionById(criteria1))

                val user2 = assertResultSuccess(repo.getUserById(userId2))
                assertEquals(UserStatus.USER_STATUS_ACTIVE, user2.status)
                assertNull(user2.deletedAt)
                assertThat(user2.firstName).isNotEmpty()
                assertResultSuccess(criterionTableRepo.getCriterionById(criteria2))
            }
    }

    @Nested
    inner class GetUserIdsToDelete {
        @Test
        fun `When no cleared users exist, then an empty list is returned`() = runTest {
            val userIdsToDelete = repo.getUserIdsToDelete()

            assertThat(userIdsToDelete).isEmpty()
        }

        @Test
        fun `When cleared users exist, then the IDs of the cleared users are returned`() = runTest {
            // Manually "soft-delete" user to set the `deletedAt` date
            val userId1 = insertUserAndGetId(
                email = "user1@test.de",
                status = UserStatus.USER_STATUS_DELETED,
                deletedAt = defaultThresholdDate.minusDays(1),
            )
            insertUserAndGetId(email = "user2@test.de", status = UserStatus.USER_STATUS_ACTIVE)
            repo.clearSoftDeletedUsers(defaultThresholdDate)

            val userIdsToDelete = repo.getUserIdsToDelete()

            assertThat(userIdsToDelete).hasSize(1)
            assertThat(userIdsToDelete).containsExactly(userId1)
        }
    }

    @Nested
    inner class HardDeleteClearedUsers {
        @Test
        fun `When no cleared users exist that have reached their threshold date to be cleared, then no users are hard-deleted`() =
            runTest {
                val userId1 = insertUserAndGetId(email = "user1@test.de", status = UserStatus.USER_STATUS_ACTIVE)
                val userId2 = insertUserAndGetId(email = "user2@test.de", status = UserStatus.USER_STATUS_ACTIVE)
                repo.softDeleteUser(userId1)
                val usersToDelete = repo.getUserIdsToDelete()

                repo.hardDeleteClearedUsers(usersToDelete)

                val user1 = assertResultSuccess(repo.getUserById(userId1))
                assertEquals(UserStatus.USER_STATUS_DELETED, user1.status)
                assertNotNull(user1.deletedAt)
                assertThat(user1.firstName).isNotEmpty()

                val user2 = assertResultSuccess(repo.getUserById(userId2))
                assertEquals(UserStatus.USER_STATUS_ACTIVE, user2.status)
                assertNull(user2.deletedAt)
                assertThat(user2.firstName).isNotEmpty()
            }

        @Test
        fun `When cleared users exist that have reached their threshold data to be cleared and are cleared, then they are hard-deleted`() =
            runTest {
                // Manually "soft-delete" user to set the `deletedAt` date
                val userId1 = insertUserAndGetId(
                    email = "user1@test.de",
                    status = UserStatus.USER_STATUS_DELETED,
                    deletedAt = defaultThresholdDate.minusDays(1),
                )
                val userId2 = insertUserAndGetId(email = "user2@test.de", status = UserStatus.USER_STATUS_ACTIVE)
                repo.clearSoftDeletedUsers(defaultThresholdDate)
                val usersToDelete = repo.getUserIdsToDelete()

                repo.hardDeleteClearedUsers(usersToDelete)

                assertResultFailure<NotFoundException>(repo.getUserById(userId1))

                val user2 = assertResultSuccess(repo.getUserById(userId2))
                assertEquals(UserStatus.USER_STATUS_ACTIVE, user2.status)
                assertNull(user2.deletedAt)
                assertThat(user2.firstName).isNotEmpty()
            }

        @Test
        fun `When a cleared user exists that is still referenced, then this user is not hard-deleted`() = runTest {
            // Manually "soft-delete" user to set the `deletedAt` date
            val userId = insertUserAndGetId(
                status = UserStatus.USER_STATUS_DELETED,
                deletedAt = defaultThresholdDate.minusDays(1),
            )
            insertProjectAndGetId(createdBy = userId)
            repo.clearSoftDeletedUsers(defaultThresholdDate)
            val usersToDelete = repo.getUserIdsToDelete()

            repo.hardDeleteClearedUsers(usersToDelete)

            val user = assertResultSuccess(repo.getUserById(userId))
            assertEquals(UserStatus.USER_STATUS_UNSPECIFIED, user.status)
            assertNotNull(user.deletedAt)
            assertThat(user.deletedAt).isBefore(defaultThresholdDate)
            assertThat(user.firstName).isEmpty()
        }
    }

    @Nested
    inner class GetPasswordHashByEmail {
        @Test
        fun `When a user is found, then a successful result with the password hash is returned`() = runTest {
            val passwordHash = "hashedPassword"
            insertUserAndGetId(email = "test.user@example.com", passwordHash = passwordHash)
            val result = repo.getPasswordHashByEmail("test.user@example.com")

            val retrievedPasswordHash = assertResultSuccess(result)
            assertEquals(passwordHash, retrievedPasswordHash)
        }

        @Test
        fun `When a user is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getPasswordHashByEmail("nonexistent email")

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class UpdatePasswordHash {
        @Test
        fun `When a user exists, then the password hash is updated`() = runTest {
            val userId = insertUserAndGetId(email = "test.user@example.com", passwordHash = "oldHash")
            val newHash = "newHash"

            repo.updatePasswordHash(userId, newHash)

            val updatedHash = assertResultSuccess(repo.getPasswordHashByEmail("test.user@example.com"))
            assertEquals(newHash, updatedHash)
        }

        @Test
        fun `When a user does not exist, then no exception is thrown`() = runTest {
            assertDoesNotThrow {
                repo.updatePasswordHash(UUID.randomUUID(), "newHash")
            }
        }
    }

    @Nested
    inner class GetUserSettings {
        @Test
        fun `When a user is found, then a successful result with the user settings is returned`() = runTest {
            val userId = insertUserAndGetId()

            val result = repo.getUserSettings(userId)

            val userSettings = assertResultSuccess(result)
            assertTrue(userSettings.areHotkeysShown)
            assertFalse(userSettings.isReviewModeEnabled)
            assertThat(userSettings.criteriaIds).isEmpty()
            assertEquals(0F, userSettings.similarityThreshold)
            assertEquals(2, userSettings.decisionMatrix.numberOfReviewers)
            assertNotEquals(
                ReviewDecisionMatrix.getDefaultInstance().toByteArray(),
                userSettings.decisionMatrix.toByteArray(),
            )
            assertThat(userSettings.fetchers).isEmpty()
            assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, userSettings.snowballingType)
            assertTrue(userSettings.reviewMaybeAllowed)
        }

        @Test
        fun `When a user is not found, then a failed result with a NotFoundException is returned`() = runTest {
            assertResultFailure<NotFoundException>(repo.getUserSettings(UUID.randomUUID()))
        }
    }

    @Nested
    inner class GetUsersMatchingSearchQuery {
        @Test
        fun `When a user is matching the search query, then this user is returned`() = runTest {
            val userId1 = insertUserAndGetId(firstName = "Johnathan", email = "jonathan.doe@example.com")
            val userId2 = insertUserAndGetId(lastName = "John", email = "doe.john@example.com")
            val userId3 = insertUserAndGetId(email = "john@example.com")

            val matchingUsers = repo.getUsersMatchingSearchQuery("john", emptySet())

            assertThat(matchingUsers).hasSize(3)

            assertThat(matchingUsers.map { it.id }).containsExactlyInAnyOrder(userId1, userId2, userId3)
        }

        @Test
        fun `When a deleted user is matching the search query, then this user is not returned`() = runTest {
            insertUserAndGetId(firstName = "john", lastName = "doe", status = UserStatus.USER_STATUS_DELETED)

            val matchingUsers = repo.getUsersMatchingSearchQuery("john", emptySet())

            assertThat(matchingUsers).hasSize(0)
        }

        @Test
        fun `When no user is matching the search query, then an empty list is returned`() = runTest {
            insertUserAndGetId(firstName = "johnathan")

            val matchingUsers = repo.getUsersMatchingSearchQuery("nonexistent", emptySet())

            assertThat(matchingUsers).hasSize(0)
        }

        @Test
        fun `When more than 10 users match the search query, then only the first 10 matching users are returned`() =
            runTest {
                for (i in 1..15) {
                    insertUserAndGetId(firstName = "John the $i-th", email = "john$i@example.com")
                }

                val matchingUsers = repo.getUsersMatchingSearchQuery("john", emptySet())

                assertThat(matchingUsers).hasSize(10)
            }

        @Test
        fun `When a user is matching the search query but should be excluded, then this user is not returned`() =
            runTest {
                val userEmail = "johnathan@example.com"
                insertUserAndGetId(email = userEmail, firstName = "johnathan")

                val matchingUsers = repo.getUsersMatchingSearchQuery("john", setOf(userEmail))

                assertThat(matchingUsers).hasSize(0)
            }
    }
}
