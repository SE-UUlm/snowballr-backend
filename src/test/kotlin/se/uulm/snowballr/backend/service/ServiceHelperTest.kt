package se.uulm.snowballr.backend.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingUserIdException
import se.uulm.snowballr.backend.repository.IUserTableRepo

class ServiceHelperTest {
    @Nested
    inner class WithUser {
        val userRepoMock = mockk<IUserTableRepo>()

        @BeforeEach
        fun setupTest() {
            RequestContext.bind(RequestContext())
        }

        @AfterEach
        fun tearDownTest() {
            RequestContext.unbind()
        }

        @Test
        fun `When no authenticated user is in the context, then an exception is thrown`() = runTest {
            assertThrows<MissingUserIdException> { withUser(userRepoMock) { } }
        }

        @Test
        fun `When retrieving the user fails, then an exception is thrown`() = runTest {
            val currentUser = DataBuilder.createExampleUser()
            RequestContext.current().userId = currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } throws TestSpecificException()

            assertThrows<TestSpecificException> { withUser(userRepoMock) { } }
        }

        @Test
        fun `When a user is retrieved successfully, then the block is executed with the current user`() = runTest {
            val currentUser = DataBuilder.createExampleUser()
            RequestContext.current().userId = currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)

            withUser(userRepoMock) {
                assertEquals(currentUser, it)
            }
        }
    }
}
