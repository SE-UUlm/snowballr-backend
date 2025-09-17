package se.uulm.snowballr.backend.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
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
        assertEquals(true, DummyUser.areHotkeysShown)
        assertEquals(false, DummyUser.isReviewModeEnabled)
        assertEquals(emptyList<UUID>(), DummyUser.criteriaIds)
        assertEquals(0F, DummyUser.similarityThreshold)
        assertTrue(ReviewDecisionMatrix.getDefaultInstance().toByteArray().contentEquals(DummyUser.decisionMatrix))
        assertEquals(emptyMap<String, Map<String, String>>(), DummyUser.fetchers)
        assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, DummyUser.snowballingType)
        assertEquals(true, DummyUser.reviewMaybeAllowed)

        assertTrue(
            PasswordUtils.verifyPassword(DummyUser.password, DummyUser.passwordHash),
            "DummyUser.passwordHash does not match DummyUser.password",
        )

        assertEquals(UserRole.USER_ROLE_ADMIN, DummyUser.role)
        assertEquals(UserStatus.USER_STATUS_ACTIVE, DummyUser.status)
    }
}
