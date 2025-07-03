package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Authentication
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class UserTableRepoTest : H2DatabaseTest(arrayOf(UserTable)) {
    private val repo = UserTableRepo(db)

    @Nested
    inner class GetUserById {
        @Test
        fun `When a user is found, then the correct user is returned`() = testCoroutine {
            val userId =
                db
                    .dbQuery {
                        UserTable.insertAndGetId {
                            it[email] = "test.user@example.com"
                            it[firstName] = "Test"
                            it[lastName] = "User"
                            it[passwordHash] = "hashedPassword"
                            it[role] = UserRole.USER_ROLE_DEFAULT
                            it[status] = UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
                        }
                    }.value

            val user = repo.getUserById(userId)

            assertThat(user.id).isEqualTo(userId)
            assertThat(user.email).isEqualTo("test.user@example.com")
            assertThat(user.firstName).isEqualTo("Test")
            assertThat(user.lastName).isEqualTo("User")
            assertThat(user.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
            assertThat(user.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
        }

        @Test
        fun `When a user is not found, then an exception is thrown`() = testCoroutine {
            assertThrows<NotFoundException> { repo.getUserById(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class GetUserByEmail {
        @Test
        fun `When a user is found, then the correct user is returned`() = testCoroutine {
            val userId =
                db
                    .dbQuery {
                        UserTable.insertAndGetId {
                            it[email] = "test.user@example.com"
                            it[firstName] = "Test"
                            it[lastName] = "User"
                            it[passwordHash] = "hashedPassword"
                            it[role] = UserRole.USER_ROLE_DEFAULT
                            it[status] = UserStatus.USER_STATUS_ACTIVE
                        }
                    }.value

            val user = repo.getUserByEmail("test.user@example.com")

            assertThat(user.id).isEqualTo(userId)
            assertThat(user.email).isEqualTo("test.user@example.com")
            assertThat(user.firstName).isEqualTo("Test")
            assertThat(user.lastName).isEqualTo("User")
            assertThat(user.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
            assertThat(user.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE)
        }

        @Test
        fun `When a user is not found, then an exception is thrown`() = testCoroutine {
            assertThrows<NotFoundException> { repo.getUserByEmail("non-existing email") }
        }
    }

    @Nested
    inner class DoesUserExistByEmail {
        @Test
        fun `When a user with the given email exists, then true is returned`() = testCoroutine {
            db.dbQuery {
                UserTable.insertAndGetId {
                    it[email] = "test.user@example.com"
                    it[firstName] = "Test"
                    it[lastName] = "User"
                    it[passwordHash] = "hashedPassword"
                    it[role] = UserRole.USER_ROLE_DEFAULT
                    it[status] = UserStatus.USER_STATUS_ACTIVE
                }
            }

            assertTrue(repo.doesUserExistByEmail("test.user@example.com"))
        }

        @Test
        fun `When a user with the given email does not exist, then false is returned`() = testCoroutine {
            assertFalse(repo.doesUserExistByEmail("test.user@example.com"))
        }
    }

    @Nested
    inner class GetAllUsers {
        @Test
        fun `When users are found, then all users are returned`() = testCoroutine {
            val userId1 =
                db
                    .dbQuery {
                        UserTable.insertAndGetId {
                            it[email] = "test.user1@example.com"
                            it[firstName] = "Test"
                            it[lastName] = "User 2"
                            it[passwordHash] = "hashedPassword"
                            it[role] = UserRole.USER_ROLE_DEFAULT
                            it[status] = UserStatus.USER_STATUS_ACTIVE
                        }
                    }.value

            val userId2 =
                db
                    .dbQuery {
                        UserTable.insertAndGetId {
                            it[email] = "test.user2@example.com"
                            it[firstName] = "Test"
                            it[lastName] = "User 1"
                            it[passwordHash] = "hashedPassword"
                            it[role] = UserRole.USER_ROLE_DEFAULT
                            it[status] = UserStatus.USER_STATUS_ACTIVE
                        }
                    }.value

            val users = repo.getAllUsers()
            assertThat(users).hasSize(2)
            val firstUser = users.find { it.id == userId1 }
            assertThat(firstUser).isNotNull
            val secondUser = users.find { it.id == userId2 }
            assertThat(secondUser).isNotNull
        }
    }

    @Nested
    inner class CreateUser {
        @Test
        fun `When a user is created, then the user is returned`() = testCoroutine {
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
        fun `When a user with an existing email is created, then an exception is thrown`() = testCoroutine {
            val request =
                Authentication.RegisterRequest
                    .newBuilder()
                    .setEmail("alice.smith@example.com")
                    .setFirstName("Alice")
                    .setLastName("Smith")
                    .setPassword("AAbb__00")
                    .build()
            repo.createUser(request, "hashedPassword")

            assertThrows<ExposedSQLException> {
                repo.createUser(request, "hashedPassword2")
            }
        }

        @Test
        fun `When two users with different emails are created, then they have different IDs`() = testCoroutine {
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
}
