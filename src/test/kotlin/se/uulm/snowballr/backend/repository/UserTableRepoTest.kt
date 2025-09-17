package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.toGrpcUser
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
import kotlin.test.assertEquals

class UserTableRepoTest : RepositoryTest(arrayOf(UserTable)) {
    private val repo = UserTableRepo(db)

    @Suppress("LongParameterList")
    private suspend fun insertTestUserAndGetId(
        email: String = "test.user@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        passwordHash: String = "passwordHash",
        role: UserRole = UserRole.USER_ROLE_DEFAULT,
        status: UserStatus = UserStatus.USER_STATUS_ACTIVE,
    ): UUID = db.query {
        UserTable.insertAndGetId {
            it[UserTable.email] = email
            it[UserTable.firstName] = firstName
            it[UserTable.lastName] = lastName
            it[UserTable.passwordHash] = passwordHash
            it[UserTable.role] = role
            it[UserTable.status] = status
        }.value
    }

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
            val userId = insertTestUserAndGetId()
            val result = repo.getUserById(userId)

            val user = assertResultSuccess(result)
            assertThat(user.id).isEqualTo(userId)
            assertThat(user.email).isEqualTo("test.user@example.com")
            assertThat(user.firstName).isEqualTo("Test")
            assertThat(user.lastName).isEqualTo("User")
            assertThat(user.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
            assertThat(user.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE)
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
                val userId = insertTestUserAndGetId()
                val result = repo.getUserByEmail("test.user@example.com")

                val user = assertResultSuccess(result)
                assertThat(user.id).isEqualTo(userId)
                assertThat(user.email).isEqualTo("test.user@example.com")
                assertThat(user.firstName).isEqualTo("Test")
                assertThat(user.lastName).isEqualTo("User")
                assertThat(user.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
                assertThat(user.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE)
            }

        @Test
        fun `When a user is not found by their email, then a failed result with a NotFoundException is returned`() =
            runTest {
                val result = repo.getUserByEmail("nonexistent email")

                assertResultFailure<NotFoundException>(result)
            }
    }

    @Nested
    inner class DoesUserExistByEmail {
        @Test
        fun `When a user with the given email exists, then true is returned`() = runTest {
            insertTestUserAndGetId()

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
            val userId1 = insertTestUserAndGetId(email = "test.user1@example.com", lastName = "User 2")
            val userId2 = insertTestUserAndGetId(email = "test.user2@example.com", lastName = "User 1")

            val users = repo.getAllUsers()
            assertThat(users).hasSize(2)
            val firstUser = users.find { it.id == userId1 }
            assertThat(firstUser).isNotNull
            val secondUser = users.find { it.id == userId2 }
            assertThat(secondUser).isNotNull
        }

        @Test
        fun `When permanently deleted users exist, then only the existent users are returned`() = runTest {
            val userId1 = insertTestUserAndGetId(email = "test.user1@example.com", lastName = "User 1")
            val userId2 = insertTestUserAndGetId(email = "", firstName = "", lastName = "")

            val users = repo.getAllUsers()
            assertThat(users).hasSize(1)
            val firstUser = users.find { it.id == userId1 }
            assertThat(firstUser).isNotNull
            val secondUser = users.find { it.id == userId2 }
            assertThat(secondUser).isNull()
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

            assertThat(user.email).isEqualTo("alice.smith@example.com")
            assertThat(user.firstName).isEqualTo("Alice")
            assertThat(user.lastName).isEqualTo("Smith")
            assertThat(user.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
            assertThat(user.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
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

            assertThat(user1.id).isNotEqualTo(user2.id)
        }
    }

    @Nested
    inner class UpdateUser {
        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.UserTableRepoTest#validFieldMasks")
        fun `When a user is updated, then only the fields specified in the field mask are updated and the updated user is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val userId = insertTestUserAndGetId(email = "test.user@example.com")
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
                assertThat(updatedUser.email).isEqualTo("updated.user@example.com")
            } else {
                assertThat(updatedUser.email).isEqualTo("test.user@example.com")
            }
            if ("user.first_name" in fieldMask) {
                assertThat(updatedUser.firstName).isEqualTo("John")
            } else {
                assertThat(updatedUser.firstName).isEqualTo("Test")
            }
            if ("user.last_name" in fieldMask) {
                assertThat(updatedUser.lastName).isEqualTo("Doe")
            } else {
                assertThat(updatedUser.lastName).isEqualTo("User")
            }
            if ("user.role" in fieldMask) {
                assertThat(updatedUser.role).isEqualTo(UserRole.USER_ROLE_ADMIN)
            } else {
                assertThat(updatedUser.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
            }
            if ("user.status" in fieldMask) {
                assertThat(updatedUser.status).isEqualTo(UserStatus.USER_STATUS_DELETED)
            } else {
                assertThat(updatedUser.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE)
            }
        }

        @Test
        fun `When a user's email should be updated to an existent email, then an SQLException is thrown`() = runTest {
            insertTestUserAndGetId(email = "alice.smith@example.com")

            val user2Id = insertTestUserAndGetId(email = "bob.smith@example.com")
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
    inner class DeleteUser {
        @Test
        fun `When the user is found, then the status of the user is set to USER_STATUS_DELETED`() = runTest {
            val userId1 = insertTestUserAndGetId()
            val before = OffsetDateTime.now()

            repo.softDeleteUser(userId1)

            val after = OffsetDateTime.now()
            val deletedUser = repo.getUserById(userId1).getOrThrow()

            assertThat(deletedUser.status).isEqualTo(UserStatus.USER_STATUS_DELETED)
            assertThat(deletedUser.deletedAt).isBetween(before, after)
        }

        @Test
        fun `When a user is not found, then no exception is thrown`() = runTest {
            assertDoesNotThrow { repo.softDeleteUser(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class GetPasswordHashByEmail {
        @Test
        fun `When a user is found, then a successful result with the password hash is returned`() = runTest {
            val passwordHash = "hashedPassword"
            insertTestUserAndGetId(email = "test.user@example.com", passwordHash = passwordHash)
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
    inner class GetUserSettings {
        @Test
        fun `When a user is found, then a successful result with the user settings is returned`() = runTest {
            val userId = insertTestUserAndGetId()
            val result = repo.getUserSettings(userId)

            val userSettings = assertResultSuccess(result)
            assertThat(userSettings.areHotkeysShown).isTrue()
            assertThat(userSettings.isReviewModeEnabled).isFalse()
            assertThat(userSettings.criteriaIds).isEmpty()
            assertThat(userSettings.similarityThreshold).isEqualTo(0F)
            assertThat(userSettings.decisionMatrix).isEqualTo(ReviewDecisionMatrix.getDefaultInstance())
            assertThat(userSettings.fetchers).isEmpty()
            assertThat(userSettings.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            assertThat(userSettings.reviewMaybeAllowed).isTrue()
        }

        @Test
        fun `When a user is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getUserSettings(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class GetUsersMatchingSearchQuery {
        @Test
        fun `When a user is matching the search query, then this user is returned`() = runTest {
            val userId1 = insertTestUserAndGetId(firstName = "Johnathan", email = "jonathan.doe@example.com")
            val userId2 = insertTestUserAndGetId(lastName = "John", email = "doe.john@example.com")
            val userId3 = insertTestUserAndGetId(email = "john@example.com")

            val matchingUsers = repo.getUsersMatchingSearchQuery("john", emptySet())

            assertThat(matchingUsers).hasSize(3)

            assertThat(matchingUsers.map { it.id }).containsExactlyInAnyOrder(userId1, userId2, userId3)
        }

        @Test
        fun `When a deleted user is matching the search query, then this user is not returned`() = runTest {
            insertTestUserAndGetId(firstName = "john", lastName = "doe", status = UserStatus.USER_STATUS_DELETED)

            val matchingUsers = repo.getUsersMatchingSearchQuery("john", emptySet())

            assertEquals(0, matchingUsers.size)
        }

        @Test
        fun `When no user is matching the search query, then an empty list is returned`() = runTest {
            insertTestUserAndGetId(firstName = "johnathan")

            val matchingUsers = repo.getUsersMatchingSearchQuery("nonexistent", emptySet())

            assertEquals(0, matchingUsers.size)
        }

        @Test
        fun `When more than 10 users match the search query, then only the first 10 matching users are returned`() =
            runTest {
                for (i in 1..15) {
                    insertTestUserAndGetId(firstName = "John the $i-th", email = "john$i@example.com")
                }

                val matchingUsers = repo.getUsersMatchingSearchQuery("john", emptySet())

                assertEquals(10, matchingUsers.size)
            }

        @Test
        fun `When a user is matching the search query but should be excluded, then this user is not returned`() =
            runTest {
                val userId = insertTestUserAndGetId(firstName = "johnathan")

                val matchingUsers = repo.getUsersMatchingSearchQuery("john", setOf(userId))

                assertEquals(0, matchingUsers.size)
            }
    }
}
