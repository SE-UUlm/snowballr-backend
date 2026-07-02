package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.UserIdentifierType
import kotlin.test.assertEquals

class GetUserByEmailTest : UserServiceTest() {
    @Test
    fun `When the current user requests themselves, then user is not requested again`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)

        val requestedUser = service.getUserByEmail(currentUser.email)

        assertEquals(currentUser.id, requestedUser.id)
        coVerify(exactly = 0) { userRepoMock.getUserByEmail(currentUser.email) }
    }

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUserEmail = "otherUser@example.com"

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(requestedUserEmail) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getUserByEmail(requestedUserEmail) }
    }

    @Test
    fun `When a user requests a user, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(email = "otherUser@example.com")

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(requestedUser.email) } returns Result.success(requestedUser)
        coEvery {
            userAccessCheckerMock.isAllowedToReadUser(currentUser, requestedUser, UserIdentifierType.EMAIL)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.getUserByEmail(requestedUser.email) }
    }

    @Test
    fun `When a user requests a user and has access, then the user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(email = "otherUser@example.com")

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(requestedUser.email) } returns Result.success(requestedUser)
        coJustRun { userAccessCheckerMock.isAllowedToReadUser(currentUser, requestedUser, UserIdentifierType.EMAIL) }

        val result = service.getUserByEmail(requestedUser.email)

        assertEquals(requestedUser.email, result.email)
    }
}
