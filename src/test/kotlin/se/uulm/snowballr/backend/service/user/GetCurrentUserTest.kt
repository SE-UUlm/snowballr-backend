package se.uulm.snowballr.backend.service.user

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest

class GetCurrentUserTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)

        assertDoesNotThrow { mainService.getCurrentUser() }
    }
}
