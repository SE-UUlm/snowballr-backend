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
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Authentication

class RegisterTest : MainServiceTest() {
    @Test
    fun `When a user with the given email already exists, then an exception is thrown`() = runTest {
        val request = Authentication.RegisterRequest.newBuilder().setEmail("test@example.com").build()

        coEvery { userRepoMock.doesUserExistByEmail("test@example.com") } returns true

        assertThrows<DuplicateEntityException> { mainService.register(request) }
    }

    @Test
    fun `When creating the user fails, then an exception is thrown`() = runTest {
        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.register(request) }
    }

    @Test
    fun `When saving the verification token fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } returns user
        coEvery { verificationTokenRepoMock.saveVerificationToken(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.register(request) }
    }

    @Test
    fun `When sending the verification email fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val request = Authentication.RegisterRequest.newBuilder().setEmail(user.email).build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } returns user
        coEvery { verificationTokenRepoMock.saveVerificationToken(any(), any()) } returns Unit
        every { emailManagerMock.createVerificationLink(any()) } returns "test-token"
        coEvery { emailManagerMock.sendVerificationEmail(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.register(request) }
    }

    @Test
    fun `When a user provides valid registration data, then the user is created and a verification email is sent`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val request = Authentication.RegisterRequest.newBuilder()
                .setEmail(user.email)
                .setFirstName(user.firstName)
                .setLastName(user.lastName)
                .setPassword("VALIDPassword__1234")
                .build()
            val testFrontendURL = "https://example.com"

            val tokenSlot = slot<String>()
            val emailDataSlot = slot<EmailData.EmailVerification>()

            coEvery { userRepoMock.doesUserExistByEmail(request.email) } returns false
            coEvery { userRepoMock.createUser(eq(request), any()) } returns user
            coEvery { verificationTokenRepoMock.saveVerificationToken(user.id, capture(tokenSlot)) } returns Unit
            every { emailManagerMock.createVerificationLink(any()) } answers {
                val token = firstArg<String>()
                "$testFrontendURL/verifyemail?token=$token"
            }
            coEvery { emailManagerMock.sendVerificationEmail(user.email, capture(emailDataSlot)) } returns Unit

            assertDoesNotThrow { mainService.register(request) }

            val capturedToken = tokenSlot.captured
            assertThat(capturedToken).isNotBlank()

            val capturedEmailData = emailDataSlot.captured
            val expectedVerificationLink = "$testFrontendURL/verifyemail?token=$capturedToken"

            assertThat(capturedEmailData.firstName).isEqualTo(user.firstName)
            assertThat(capturedEmailData.lastName).isEqualTo(user.lastName)
            assertThat(capturedEmailData.verificationLink).isEqualTo(expectedVerificationLink)
        }
}
