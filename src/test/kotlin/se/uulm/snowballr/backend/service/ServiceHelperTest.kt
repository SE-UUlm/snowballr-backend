package se.uulm.snowballr.backend.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.repository.IUserTableRepo
import java.util.UUID

class ServiceHelperTest {
    @Nested
    inner class WithUser {
        val userRepoMock = mockk<IUserTableRepo>()

        @BeforeEach
        fun setupTest() {
            mockkObject(GrpcContext)
        }

        @Test
        fun `When retrieving the user ID fails, then an exception is thrown`() = runTest {
            every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

            assertThrows<TestSpecificException> { withUser(userRepoMock) { } }
        }

        @Test
        fun `When retrieving the user fails, then an exception is thrown`() = runTest {
            val currentUserId = UUID.randomUUID()
            every { GrpcContext.getUserIdFromContext() } returns currentUserId
            coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

            assertThrows<TestSpecificException> { withUser(userRepoMock) { } }
        }

        @Test
        fun `When the user is retrieved successfully, then the block is executed with the current user`() = runTest {
            val currentUser = DataBuilder.createExampleUser()
            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)

            assertDoesNotThrow {
                withUser(userRepoMock) {
                    assertEquals(currentUser, it)
                }
            }
        }
    }
}
