package se.uulm.snowballr.backend.service.user

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder

class GetCurrentUserTest : UserServiceTest() {
    @Test
    fun `When retrieving the current user, then the correct user is returned`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)

        val result = service.getCurrentUser()

        assertEquals(user.id, result.id)
    }
}
