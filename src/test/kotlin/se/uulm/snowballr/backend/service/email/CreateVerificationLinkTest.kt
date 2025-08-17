package se.uulm.snowballr.backend.service.email

import com.github.jknack.handlebars.Template
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simplejavamail.api.mailer.Mailer
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.email.EmailTemplate
import se.uulm.snowballr.backend.service.EmailService
import se.uulm.snowballr.backend.service.IEmailService

class CreateVerificationLinkTest {
    private val envReaderMock = mockk<EnvReader>()
    private val mailerMock = mockk<Mailer>()
    private val compiledTemplatesMock = mockk<Map<EmailTemplate, Template>>()
    private lateinit var emailService: IEmailService

    private val testFrontendUrl = "https://example.com"

    @BeforeEach
    fun setUp() {
        val miscellaneousMock = mockk<Env.Miscellaneous>()
        every { miscellaneousMock.frontendBaseUrl } returns testFrontendUrl

        val envMock = mockk<Env>()
        every { envMock.miscellaneous } returns miscellaneousMock

        every { envReaderMock.env } returns envMock

        emailService = EmailService(envReaderMock, mailerMock, compiledTemplatesMock)
    }

    @Test
    fun `When given a token, then the correct verification link is created`() {
        val verificationToken = "random-test-token"
        val expectedLink = "$testFrontendUrl/verifyemail?token=$verificationToken"

        val actualLink = emailService.createVerificationLink(verificationToken)

        assertThat(actualLink).isEqualTo(expectedLink)
    }
}
