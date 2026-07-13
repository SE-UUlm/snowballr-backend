package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.notfound.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import java.time.OffsetDateTime

class VerifyEmailTest : AuthenticationServiceTest() {
    @Test
    fun `When the verification token is not found, then a VerificationTokenNotFoundException is thrown`() = runTest {
        val token = "non-existent-token"

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token) } returns Result.failure(
            VerificationTokenNotFoundException(),
        )

        assertThrows<VerificationTokenNotFoundException> { service.verifyEmail(token) }
    }

    @Test
    fun `When the verification token has expired, then a VerificationTokenNotFoundException is thrown`() = runTest {
        val expiredToken = DataBuilder.createExampleVerificationToken(
            expiresAt = OffsetDateTime.now().minusMinutes(1),
        )

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(expiredToken.token) } returns Result.success(
            expiredToken,
        )
        coJustRun { verificationTokenRepoMock.deleteVerificationToken(expiredToken.token) }

        assertThrows<VerificationTokenNotFoundException> { service.verifyEmail(expiredToken.token) }
    }

    @Test
    fun `When the user associated with the token is not found, then a TestSpecificException is thrown`() = runTest {
        val token = DataBuilder.createExampleVerificationToken()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserById(token.userId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.verifyEmail(token.token) }
    }

    @Test
    fun `When a valid token is provided and all operations succeed, then the token is successfully deleted afterwards`() =
        runTest {
            val user = DataBuilder.createExampleUser(status = UserStatus.ACTIVE_UNCONFIRMED)
            val token = DataBuilder.createExampleVerificationToken(userId = user.id)
            val userUpdateSlot = slot<UpdateUserRequest>()

            coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns Result.success(token)
            coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
            coEvery { userRepoMock.updateUser(capture(userUpdateSlot), any()) } returns user
            coJustRun { verificationTokenRepoMock.deleteVerificationToken(token.token) }

            service.verifyEmail(token.token)

            coVerify(exactly = 1) {
                verificationTokenRepoMock.deleteVerificationToken(token.token)
            }
        }
}
