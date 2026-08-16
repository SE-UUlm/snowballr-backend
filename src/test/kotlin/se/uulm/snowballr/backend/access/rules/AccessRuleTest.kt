package se.uulm.snowballr.backend.access.rules

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.internal.AccessRuleCheckFailedException

class AccessRuleTest {
    @Nested
    inner class CheckFor {
        @Test
        fun `When an access rule evaluates to false, then an AccessRuleCheckFailedException is thrown`() = runTest {
            val accessRule = AccessRule<Unit> { _, _ -> false }
            val user = DataBuilder.createExampleUser()

            assertThrows<AccessRuleCheckFailedException> { accessRule.checkFor(user) }
        }
    }
}
