package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class GetUserByIdTest : MainServiceTest() {
    @Test
    fun `When a user is correctly retrieved, then no exception is thrown`() =
        testCoroutine {
            val request = Base.Id.newBuilder().build()
            val user = DataBuilder.createExampleUser()

            coEvery { userRepoMock.getUserById(any()) } returns user

            assertDoesNotThrow { mainService.getUserById(request) }
        }

    @Test
    fun `When an error occurs while a user is retrieved, then an exception is thrown`() =
        testCoroutine {
            val request = Base.Id.newBuilder().build()

            coEvery { userRepoMock.getUserById(any()) } throws Exception("Failed to retrieve user")

            assertThrows<Exception> { mainService.getUserById(request) }
        }
}
