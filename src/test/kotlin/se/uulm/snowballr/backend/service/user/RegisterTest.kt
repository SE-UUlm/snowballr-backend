package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import snowballr.Authentication

class RegisterTest : UserServiceTest() {
    private fun getExampleRequest(user: User) = Authentication.RegisterRequest.newBuilder()
        .setEmail(user.email)
        .setFirstName(user.firstName)
        .setLastName(user.lastName)
        .setPassword("VALIDPassword__1234")
        .build()

    @Test
    fun `When a user with the given email already exists, then a DuplicateUserException is thrown`() = runTest {
        val existentEmail = "existent-email"
        val request = Authentication.RegisterRequest.newBuilder()
            .setEmail(existentEmail)
            .build()

        coEvery { userRepoMock.doesUserExistByEmail(existentEmail) } returns true

        assertThrows<DuplicateUserException> { service.register(request) }
    }

    @Test
    fun `When sending the verification email fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val verificationLink = "verification-link"
        val userData = EmailData.EmailVerification(user.firstName, verificationLink, "tomorrow")

        coEvery { userRepoMock.doesUserExistByEmail(user.email) } returns false
        coEvery { userRepoMock.createUser(getExampleRequest(user), any()) } returns user
        coJustRun { verificationTokenRepoMock.saveVerificationToken(user.id, any()) }
        every { emailManagerMock.createVerificationLink(any()) } returns verificationLink
        every { envReaderMock.env.lifetime.verificationTokenLifeTimeInDays } returns 1
        coEvery { emailManagerMock.sendVerificationEmail(user.email, userData) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.register(getExampleRequest(user)) }
    }

    @Test
    fun `When a user provides valid registration data, then the user is created and a verification email is sent`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val testFrontendURL = "https://example.com"

            val tokenSlot = slot<String>()
            val emailDataSlot = slot<EmailData.EmailVerification>()

            coEvery { userRepoMock.doesUserExistByEmail(user.email) } returns false
            coEvery { userRepoMock.createUser(getExampleRequest(user), any()) } returns user
            coJustRun { verificationTokenRepoMock.saveVerificationToken(user.id, capture(tokenSlot)) }
            every { emailManagerMock.createVerificationLink(any()) } answers {
                val token = firstArg<String>()
                "$testFrontendURL/verifyemail?token=$token"
            }
            every { envReaderMock.env.lifetime.verificationTokenLifeTimeInDays } returns 7
            coJustRun { emailManagerMock.sendVerificationEmail(user.email, capture(emailDataSlot)) }

            assertDoesNotThrow { service.register(getExampleRequest(user)) }

            val capturedToken = tokenSlot.captured
            assertThat(capturedToken).isNotBlank()

            val capturedEmailData = emailDataSlot.captured
            val expectedVerificationLink = "$testFrontendURL/verifyemail?token=$capturedToken"

            assertEquals(user.firstName, capturedEmailData.firstName)
            assertEquals(expectedVerificationLink, capturedEmailData.verificationLink)
        }
}
