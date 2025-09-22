package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Authentication

class RegisterTest : MainServiceTest() {
    private fun getExampleRequest(user: User) = Authentication.RegisterRequest.newBuilder()
        .setEmail(user.email)
        .setFirstName(user.firstName)
        .setLastName(user.lastName)
        .setPassword("VALIDPassword__1234")
        .build()

    @Test
    fun `When a user with the given email already exists, then a DuplicateEntityException is thrown`() = runTest {
        val existentEmail = "existent-email"
        val request = Authentication.RegisterRequest.newBuilder()
            .setEmail(existentEmail)
            .build()

        coEvery { userRepoMock.doesUserExistByEmail(existentEmail) } returns true

        assertThrows<DuplicateEntityException> { mainService.register(request) }
    }

    @Test
    fun `When sending the verification email fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val verificationLink = "verification-link"
        val userData = EmailData.EmailVerification(user.firstName, user.lastName, verificationLink)

        coEvery { userRepoMock.doesUserExistByEmail(user.email) } returns false
        coEvery { userRepoMock.createUser(getExampleRequest(user), any()) } returns user
        coEvery { verificationTokenRepoMock.saveVerificationToken(user.id, any()) } returns Unit
        every { emailManagerMock.createVerificationLink(any()) } returns verificationLink
        coEvery { emailManagerMock.sendVerificationEmail(user.email, userData) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.register(getExampleRequest(user)) }
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
            coEvery { verificationTokenRepoMock.saveVerificationToken(user.id, capture(tokenSlot)) } returns Unit

            every { emailManagerMock.createVerificationLink(any()) } answers {
                val token = firstArg<String>()
                "$testFrontendURL/verifyemail?token=$token"
            }

            coEvery { emailManagerMock.sendVerificationEmail(user.email, capture(emailDataSlot)) } returns Unit

            assertDoesNotThrow { mainService.register(getExampleRequest(user)) }

            val capturedToken = tokenSlot.captured
            assertThat(capturedToken).isNotBlank()

            val capturedEmailData = emailDataSlot.captured
            val expectedVerificationLink = "$testFrontendURL/verifyemail?token=$capturedToken"

            assertThat(capturedEmailData.firstName).isEqualTo(user.firstName)
            assertThat(capturedEmailData.lastName).isEqualTo(user.lastName)
            assertThat(capturedEmailData.verificationLink).isEqualTo(expectedVerificationLink)
        }
}
