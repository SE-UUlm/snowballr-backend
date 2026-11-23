package se.uulm.snowballr.backend.mail

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.email.EmailData

class EmailManagerTest {
    private val testFrontendUrl = "https://frontend.test"
    private val recipientEmail = "test.user@example.com"

    private val envReaderMock = mockk<EnvReader> {
        every { env } returns mockk<Env> {
            every { miscellaneous } returns mockk<Env.Miscellaneous> {
                every { frontendBaseUrl } returns testFrontendUrl
            }
        }
    }

    private val emailSender = DummyEmailSender()
    private val emailManager = EmailManager(envReaderMock, EmailTemplateManager(), emailSender)

    @Nested
    inner class SendEmails {
        @Test
        fun `When sending a verification email, then the email is sent with the correct content and headers`() {
            val expectedVerificationLink = emailManager.createVerificationLink("this-is-a-test-token-123")
            val emailData = EmailData.EmailVerification("John", expectedVerificationLink, "in 24 hours")

            emailManager.sendVerificationEmail(recipientEmail, emailData)

            assertThat(emailSender.getSentEmails()).singleElement().satisfies({ email ->
                assertThat(email.to).isEqualTo(recipientEmail)
                assertThat(email.subject).isEqualTo(emailData.subject)
                assertThat(email.htmlBody).contains("Hello John,")
                assertThat(email.htmlBody)
                    .contains("<a href=\"$expectedVerificationLink\">$expectedVerificationLink</a>")
                assertThat(email.htmlBody).contains("This link will expire in 24 hours.")
            })
        }

        @Test
        fun `When sending a project invitation acceptance email, then the email is sent with the correct content and headers`() {
            val expectedAcceptanceLink = emailManager.createAcceptProjectInvitationLink("this-is-a-test-token-123")
            val emailData = EmailData.AcceptProjectInvitation(
                "John",
                "Jane Doe",
                "Test Project",
                expectedAcceptanceLink,
                "in 24 hours",
            )

            emailManager.sendAcceptProjectInvitationEmail(recipientEmail, emailData)

            assertThat(emailSender.getSentEmails()).singleElement().satisfies({ email ->
                assertThat(email.to).isEqualTo(recipientEmail)
                assertThat(email.subject).isEqualTo(emailData.subject)
                assertThat(email.htmlBody).contains("Hello John,")
                assertThat(email.htmlBody).contains("Jane Doe invited you to join the project")
                assertThat(email.htmlBody).contains("Test Project")
                assertThat(email.htmlBody)
                    .contains("<a href=\"$expectedAcceptanceLink\">$expectedAcceptanceLink</a>")
                assertThat(email.htmlBody).contains("This invitation will expire in 24 hours.")
            })
        }
    }

    @Nested
    inner class CreateLinks {
        @Test
        fun `When given a token, then the correct verification link is created`() {
            val verificationToken = "random-test-token"
            val expectedLink = "$testFrontendUrl/verifyemail?token=$verificationToken"

            val actualLink = emailManager.createVerificationLink(verificationToken)

            assertEquals(expectedLink, actualLink)
        }

        @Test
        fun `When given a token, then the correct accept project invitation link is created`() {
            val acceptProjectInvitationToken = "random-test-token"
            val expectedLink = "$testFrontendUrl/acceptprojectinvitation?token=$acceptProjectInvitationToken"

            val actualLink = emailManager.createAcceptProjectInvitationLink(acceptProjectInvitationToken)

            assertEquals(expectedLink, actualLink)
        }
    }
}
