package se.uulm.snowballr.backend.mail

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simplejavamail.api.mailer.Mailer
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader

class CreateVerificationLinkTest {
    private val envReaderMock = mockk<EnvReader>()
    private val mailerMock = mockk<Mailer>()
    private val emailTemplateManagerMock = mockk<EmailTemplateManager>()
    private lateinit var emailManager: IEmailManager

    private val testFrontendUrl = "https://example.com"

    @BeforeEach
    fun setUp() {
        val miscellaneousMock = mockk<Env.Miscellaneous>()
        every { miscellaneousMock.frontendBaseUrl } returns testFrontendUrl

        val envMock = mockk<Env>()
        every { envMock.miscellaneous } returns miscellaneousMock

        every { envReaderMock.env } returns envMock

        emailManager = EmailManager(envReaderMock, mailerMock, emailTemplateManagerMock)
    }

    @Test
    fun `When given a token, then the correct verification link is created`() {
        val verificationToken = "random-test-token"
        val expectedLink = "$testFrontendUrl/verifyemail?token=$verificationToken"

        val actualLink = emailManager.createVerificationLink(verificationToken)

        assertThat(actualLink).isEqualTo(expectedLink)
    }
}
