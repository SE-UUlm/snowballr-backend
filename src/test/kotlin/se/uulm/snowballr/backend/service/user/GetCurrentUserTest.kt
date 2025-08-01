package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class GetCurrentUserTest : MainServiceTest() {
    @Test
    fun `When retrieving the user context fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getCurrentUser() }
    }

    @Test
    fun `When retrieving the user by id fails, then an exception is thrown`() = runTest {
        val userId = UUID.randomUUID()
        every { GrpcContext.getUserIdFromContext() } returns userId
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getCurrentUser() }
    }

    @Test
    fun `When retrieving the current user, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user

        assertDoesNotThrow { mainService.getCurrentUser() }
    }
}
