package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass.UserRole

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class DeleteUserTest : MainServiceTest() {
    @Test
    fun `When an unauthorized user tries to delete a user, then an unauthorized exception is thrown`() =
        testCoroutine {
            val firstUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val request =
                Base.Id
                    .newBuilder()
                    .setId(firstUser.id.toString())
                    .build()
            val secondUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            coEvery { userRepoMock.getUserById(dummyUserId!!) } returns secondUser

            assertThrows<UnauthorizedException.All.User> { mainService.softDeleteUser(request) }
        }

    @Test
    fun `When an admin user tries to delete a user, then no exception is thrown`() =
        testCoroutine {
            val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val userToDelete = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val request =
                Base.Id
                    .newBuilder()
                    .setId(userToDelete.id.toString())
                    .build()

            coEvery { userRepoMock.getUserById(dummyUserId!!) } returns adminUser

            assertDoesNotThrow { mainService.softDeleteUser(request) }
        }

    @Test
    fun `When user tries to delete himself, then no exception is thrown`() =
        testCoroutine {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val request =
                Base.Id
                    .newBuilder()
                    .setId(user.id.toString())
                    .build()

            coEvery { userRepoMock.getUserById(dummyUserId!!) } returns user

            assertDoesNotThrow { mainService.softDeleteUser(request) }
        }
}
