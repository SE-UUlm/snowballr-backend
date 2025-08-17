package se.uulm.snowballr.backend.service.user

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
import snowballr.UserOuterClass
import java.time.OffsetDateTime

class VerifyEmailTest : MainServiceTest() {
    @Test
    fun `When the verification token is not found, then an exception is thrown`() = runTest {
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken("non-existent-token").build()
        coEvery { verificationTokenRepoMock.getVerificationTokenByValue("non-existent-token") } returns null

        assertThrows<VerificationTokenNotFoundException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When the verification token has expired, then an exception is thrown`() = runTest {
        val expiredToken = DataBuilder.createExampleVerificationToken(
            expiresAt = OffsetDateTime.now().minusMinutes(1),
        )
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(expiredToken.token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(expiredToken.token) } returns expiredToken
        coEvery { verificationTokenRepoMock.deleteVerificationToken(expiredToken.token) } returns Unit

        assertThrows<VerificationTokenNotFoundException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When the user associated with the token is not found, then an exception is thrown`() = runTest {
        val token = DataBuilder.createExampleVerificationToken()
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token.token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns token
        coEvery { userRepoMock.getUserById(token.userId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When updating the user status fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val token = DataBuilder.createExampleVerificationToken(userId = user.id)
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token.token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns token
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { userRepoMock.updateUser(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When deleting the verification token fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val token = DataBuilder.createExampleVerificationToken(userId = user.id)
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token.token).build()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns token
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { userRepoMock.updateUser(any()) } returns user
        coEvery { verificationTokenRepoMock.deleteVerificationToken(token.token) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.verifyEmail(request) }
    }

    @Test
    fun `When a valid token is provided and all operations succeed, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
        val token = DataBuilder.createExampleVerificationToken(userId = user.id)
        val request = Authentication.VerifyEmailRequest.newBuilder().setToken(token.token).build()
        val userUpdateSlot = slot<UserOuterClass.User.Update>()

        coEvery { verificationTokenRepoMock.getVerificationTokenByValue(token.token) } returns token
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { userRepoMock.updateUser(capture(userUpdateSlot)) } returns user
        coEvery { verificationTokenRepoMock.deleteVerificationToken(token.token) } returns Unit

        assertDoesNotThrow { mainService.verifyEmail(request) }
    }
}
