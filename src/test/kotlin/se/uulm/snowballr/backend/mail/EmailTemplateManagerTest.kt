package se.uulm.snowballr.backend.mail

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.EmailException
import se.uulm.snowballr.backend.model.email.EmailTemplate
import java.nio.file.Files

class EmailTemplateManagerTest {
    private val emailTemplateManager = EmailTemplateManager()

    @Nested
    inner class Init {
        @Test
        fun `When the path doesn't exist, then a TemplateCompilationFailed exception is thrown`() {
            assertThrows<EmailException.TemplateCompilationFailed> {
                EmailTemplateManager("foo/bar/not/existent")
            }
        }

        @Test
        fun `When no template files are found, then a TemplateCompilationFailed exception is thrown`() {
            val tmpDir = Files.createTempDirectory("tmp-template-dir").toFile()
            tmpDir.deleteOnExit()

            assertThrows<EmailException.TemplateCompilationFailed> {
                EmailTemplateManager(tmpDir.absolutePath)
            }
        }
    }

    @Nested
    inner class GetTemplate {
        @Test
        fun `When getTemplate is called with an existent template, then the compiled template is returned`() {
            val template = emailTemplateManager.getTemplate(EmailTemplate.EMAIL_VERIFICATION)

            assertNotNull(template)
        }
    }
}
