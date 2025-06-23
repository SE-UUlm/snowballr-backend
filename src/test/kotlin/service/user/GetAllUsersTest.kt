package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class GetAllUsersTest : MainServiceTest() {
    private var dummyUserUUID: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        dummyUserUUID = UUID.fromString(dummyUserId!!)
    }

    @Test
    fun `When the requesting user has an invalid ID, then an exception is thrown`() = testCoroutine {
        dummyUserId = "invalid-UUID"

        assertThrows<InvalidIdException.UUID> { mainService.getAllUsers() }
    }

    @Test
    fun `When all users are retrieved by an admin, then no exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { userRepoMock.getAllUsers() } returns emptyList()

        assertDoesNotThrow { mainService.getAllUsers() }
    }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = testCoroutine {
        coEvery { userRepoMock.getUserById(dummyUserUUID) } throws TestSpecificException()
        coEvery { userRepoMock.getAllUsers() } returns emptyList()

        assertThrows<TestSpecificException> { mainService.getAllUsers() }
    }

    @Test
    fun `When all users are retrieved by an non-admin, then an exception is thrown`() = testCoroutine {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns nonAdminUser
        coEvery { userRepoMock.getAllUsers() } returns emptyList()

        assertThrows<UnauthorizedException.All.User> { mainService.getAllUsers() }
    }

    @Test
    fun `When retrieving all users fails, then an exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { userRepoMock.getAllUsers() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllUsers() }
    }
}
