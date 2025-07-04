package se.uulm.snowballr.backend.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import snowballr.UserOuterClass.UserRole

class ServiceChecksTest {
    @Nested
    inner class VerifyServerAdminRole {
        @ParameterizedTest
        @GrpcEnumSourceTest(UserRole::class, excludes = ["USER_ROLE_ADMIN"])
        fun `When the user is not a server admin, then an exception is thrown`(role: UserRole) {
            val user = DataBuilder.createExampleUser(role = role)

            assertThrows<UnauthorizedException.All> { verifyServerAdminRole(user, EntityType.PROJECT) }
        }

        @Test
        fun `When the user is a server admin, then no exception is thrown`() {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            assertDoesNotThrow { verifyServerAdminRole(user, EntityType.PROJECT) }
        }
    }
}
