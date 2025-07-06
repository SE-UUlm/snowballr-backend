package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class DeleteUserTest : MainServiceTest() {
    private var dummyUserUUID: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        dummyUserUUID = UUID.fromString(dummyUserId!!)
    }

    @Test
    fun `When a user who is not an admin tries to delete another user, then an unauthorized exception is thrown`() =
        testCoroutine {
            val firstUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val request =
                Base.Id
                    .newBuilder()
                    .setId(firstUser.id.toString())
                    .build()
            val secondUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns secondUser
            coEvery { userRepoMock.getUserById(parseUUID(request.id, EntityType.USER)) } returns firstUser

            assertThrows<UnauthorizedException.Single> { mainService.softDeleteUser(request) }
        }

    @Test
    fun `When an admin tries to delete another admin, then a failed precondition exception is thrown`() =
        testCoroutine {
            val firstUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val request =
                Base.Id
                    .newBuilder()
                    .setId(firstUser.id.toString())
                    .build()
            val secondUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            coEvery { userRepoMock.getUserById(any()) } returns firstUser
            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns secondUser

            assertThrows<SnowballRException.FailedPreconditionException> { mainService.softDeleteUser(request) }
        }

    @Test
    fun `When an admin tries to delete a user, then no exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val request =
            Base.Id
                .newBuilder()
                .setId(userToDelete.id.toString())
                .build()

        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { userRepoMock.getUserById(parseUUID(request.id, EntityType.USER)) } returns userToDelete

        assertDoesNotThrow { mainService.softDeleteUser(request) }
    }

    @Test
    fun `When a user tries to delete an admin user, then an unauthorized exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val normalUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val request =
            Base.Id
                .newBuilder()
                .setId(adminUser.id.toString())
                .build()

        print("Holla: " + parseUUID(request.id, EntityType.USER))
        print("Hallo: $dummyUserUUID")
        coEvery { userRepoMock.getUserById(parseUUID(request.id, EntityType.USER)) } returns adminUser
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns normalUser

        assertThrows<UnauthorizedException.Single> { mainService.softDeleteUser(request) }
    }

    @Test
    fun `When a user tries to delete himself, then no exception is thrown`() = testCoroutine {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val request =
            Base.Id
                .newBuilder()
                .setId(dummyUserId)
                .build()

        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user

        assertDoesNotThrow { mainService.softDeleteUser(request) }
    }
}
