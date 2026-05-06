package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.UserIdentifierType
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class GetUserByIdTest : MainServiceTest() {
    @Test
    fun `When the current user requests themselves, then user is not requested again`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)

        val requestedUser = mainService.getUserById(currentUser.id)

        assertEquals(currentUser.id.toString(), requestedUser.id)

        // Should not call userRepoMock.getUserById(requestedUserId) again because it's self-request
        // First time is for injecting current user into session
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
    }

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUserId = UUID.randomUUID()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getUserById(requestedUserId) }
    }

    @Test
    fun `When the user requests a user, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
        coEvery {
            userAccessCheckerMock.isAllowedToReadUser(currentUser, requestedUser, UserIdentifierType.ID)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(requestedUser.id) }
    }

    @Test
    fun `When the user requests a user and has access, then the user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
        coJustRun { userAccessCheckerMock.isAllowedToReadUser(currentUser, requestedUser, UserIdentifierType.ID) }

        val result = mainService.getUserById(requestedUser.id)

        kotlin.test.assertEquals(requestedUser.id.toString(), result.id)
    }
}
