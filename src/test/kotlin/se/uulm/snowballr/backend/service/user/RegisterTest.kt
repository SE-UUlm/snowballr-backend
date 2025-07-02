package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Authentication

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class RegisterTest : MainServiceTest() {
    @Test
    fun `When a user is registered, then no exception is thrown`() = testCoroutine {
        val request = Authentication.RegisterRequest.newBuilder().build()
        val user = DataBuilder.createExampleUser()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } returns user

        assertDoesNotThrow { mainService.register(request) }
    }

    @Test
    fun `When a user already exists, then an exception is thrown`() = testCoroutine {
        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns true

        assertThrows<SnowballRException.DuplicateEntityException.UserEmail> { mainService.register(request) }
    }

    @Test
    fun `When an error occurs while creating a user, then an exception is thrown`() = testCoroutine {
        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } throws Exception("Failed to create user")

        assertThrows<Exception> { mainService.register(request) }
    }
}
