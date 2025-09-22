package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Authentication
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import snowballr.UserOuterClass.User as GrpcUser

class VerifyEmailTest : MainServiceTest() {
    @Test
    fun `When the verification token is not found, then a VerificationTokenNotFoundException is thrown`() = runTest {
        val token = "non-existent-token"
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token) } returns null

        assertThrows<VerificationTokenNotFoundException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When the verification token has expired, then a VerificationTokenNotFoundException is thrown`() = runTest {
        val expiredToken = DataBuilder.createExampleVerificationToken(
            expiresAt = OffsetDateTime.now().minusMinutes(1),
        )
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(expiredToken.token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(expiredToken.token) } returns expiredToken
        coEvery { verificationTokenRepoMock.deleteVerificationToken(expiredToken.token) } returns Unit

        assertThrows<VerificationTokenNotFoundException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When the user associated with the token is not found, then a TestSpecificException is thrown`() = runTest {
        val token = DataBuilder.createExampleVerificationToken()
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token.token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns token
        coEvery { userRepoMock.getUserById(token.userId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When a valid token is provided and all operations succeed, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
        val token = DataBuilder.createExampleVerificationToken(userId = user.id)
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token.token).build()
        val userUpdateSlot = slot<GrpcUser.Update>()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns token
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery { userRepoMock.updateUser(capture(userUpdateSlot)) } returns user
        coEvery { verificationTokenRepoMock.deleteVerificationToken(token.token) } returns Unit

        assertDoesNotThrow { mainService.verifyEmail(request) }
    }
}
