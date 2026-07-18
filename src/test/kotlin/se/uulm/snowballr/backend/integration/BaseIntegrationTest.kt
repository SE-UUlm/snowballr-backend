package se.uulm.snowballr.backend.integration

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder

class BaseIntegrationTest : IntegrationTest() {
    @Test
    fun `When actAsUser is called, then the context is set to the specified user`() = runTest {
        val currentUser = userService.getCurrentUser()

        val otherUserData = DataBuilder.createExampleUser(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
        )
        val otherUser = addUser(otherUserData)

        actAsUser(otherUser.id) {
            val otherCurrentUser = userService.getCurrentUser()
            assertEquals(otherUser.id, otherCurrentUser.id)
        }

        val currentUserId = userService.getCurrentUser().id
        assertEquals(currentUser.id, currentUserId)
    }
}
