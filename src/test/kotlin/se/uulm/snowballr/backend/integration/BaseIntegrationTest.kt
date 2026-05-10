package se.uulm.snowballr.backend.integration

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.parseUUID
import kotlin.test.assertEquals

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
        val otherUserId = parseUUID(otherUser.id, EntityType.USER)

        actAsUser(otherUserId) {
            val otherCurrentUser = userService.getCurrentUser()
            assertEquals(otherUser.id, otherCurrentUser.id)
        }

        val currentUserId = userService.getCurrentUser().id
        assertEquals(currentUser.id, currentUserId)
    }
}
