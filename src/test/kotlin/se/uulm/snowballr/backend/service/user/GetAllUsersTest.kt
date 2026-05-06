package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest

class GetAllUsersTest : MainServiceTest() {
    @Test
    fun `When the user requests all users and has access, then all users are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()
        val users = listOf(currentUser, otherUser)

        mockCurrentUser(currentUser)
        coJustRun { userAccessCheckerMock.isAllowedToReadAllUsers(currentUser) }
        coEvery { userRepoMock.getAllUsers() } returns users

        val allUsers = mainService.getAllUsers()

        assertEquals(2, allUsers.usersCount)
        assertEquals(currentUser.id.toString(), allUsers.usersList[0].id)
        assertEquals(otherUser.id.toString(), allUsers.usersList[1].id)
    }

    @Test
    fun `When the user requests all users, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userAccessCheckerMock.isAllowedToReadAllUsers(currentUser) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllUsers() }
    }
}
