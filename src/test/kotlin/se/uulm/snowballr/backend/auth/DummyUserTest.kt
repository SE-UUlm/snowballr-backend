package se.uulm.snowballr.backend.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import java.util.UUID

class DummyUserTest {
    @Test
    fun `DummyUser has expected default values`() {
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), DummyUser.id)
        assertEquals("alice.smith@example.com", DummyUser.email)
        assertEquals("Alice", DummyUser.firstName)
        assertEquals("Smith", DummyUser.lastName)
        assertEquals("VALIDPassword__1234", DummyUser.password)
        // User Settings
        assertTrue(DummyUser.areHotkeysShown)
        assertFalse(DummyUser.isReviewModeEnabled)
        assertThat(DummyUser.criteriaIds).isEmpty()
        assertEquals(0F, DummyUser.similarityThreshold)
        assertTrue(ReviewDecisionMatrix.getDefaultInstance().toByteArray().contentEquals(DummyUser.decisionMatrix))
        assertThat(DummyUser.fetchers).isEmpty()
        assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, DummyUser.snowballingType)
        assertTrue(DummyUser.reviewMaybeAllowed)

        assertTrue(
            PasswordUtils.verifyPassword(DummyUser.password, DummyUser.passwordHash),
            "DummyUser.passwordHash does not match DummyUser.password",
        )

        assertEquals(UserRole.ADMIN, DummyUser.role)
        assertEquals(UserStatus.ACTIVE, DummyUser.status)
    }
}
