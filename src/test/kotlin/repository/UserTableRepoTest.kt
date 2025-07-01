package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.testCoroutine
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
                            it[role] = UserRole.USER_ROLE_DEFAULT
                            it[status] = UserStatus.USER_STATUS_ACTIVE
                        }
                    }.value

            val user = repo.getUserById(userId)

            assertThat(user.id).isEqualTo(userId)
            assertThat(user.email).isEqualTo("test.user@example.com")
            assertThat(user.firstName).isEqualTo("Test")
            assertThat(user.lastName).isEqualTo("User")
            assertThat(user.role).isEqualTo(UserRole.USER_ROLE_DEFAULT)
            assertThat(user.status).isEqualTo(UserStatus.USER_STATUS_ACTIVE)
        }

        @Test
        fun `When a user is not found, then an exception is thrown`() = testCoroutine {
            assertThrows<NotFoundException.User> { repo.getUserById(UUID.randomUUID()) }
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
            assertThrows<NotFoundException.User> { repo.getUserByEmail("non-existing email") }
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
}
