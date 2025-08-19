package se.uulm.snowballr.backend.mail

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import se.uulm.snowballr.backend.model.email.EmailTemplate

class GetTemplateTest {
    private val emailTemplateManager = EmailTemplateManager()

    @Test
    fun `When getTemplate is called with an existing template, then the compiled template is returned`() {
        val template = emailTemplateManager.getTemplate(EmailTemplate.EMAIL_VERIFICATION)

        assertNotNull(template)
    }
}
