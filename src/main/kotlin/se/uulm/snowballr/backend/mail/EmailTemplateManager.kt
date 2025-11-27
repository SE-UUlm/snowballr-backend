package se.uulm.snowballr.backend.mail

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import se.uulm.snowballr.backend.model.email.EmailTemplate
import se.uulm.snowballr.backend.model.exception.EmailException
import java.io.IOException

/**
 * Manages the compilation and retrieval of email templates.
 *
 * This class encapsulates the logic for loading and compiling [Handlebars] templates.
 */
class EmailTemplateManager(templatePath: String = "/templates") {
    private val templates: Map<EmailTemplate, Template>

    init {
        val handlebars = Handlebars(ClassPathTemplateLoader(templatePath, ".hbs"))

        templates = EmailTemplate.entries.associateWith { template ->
            try {
                handlebars.compile(template.templateFileName)
            } catch (e: IOException) {
                throw EmailException.TemplateCompilationFailed(template.templateFileName, e)
            }
        }
    }

    /**
     * Retrieves a compiled template for the specified email template type.
     *
     * @param emailTemplate The enum representing the desired email template.
     * @return The compiled [Template] object.
     */
    @Suppress("MapGetWithNotNullAssertionOperator", "UnsafeCallOnNullableType")
    fun getTemplate(emailTemplate: EmailTemplate): Template = templates[emailTemplate]!!
}
