package se.uulm.snowballr.backend.mail

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.email.EmailTemplate
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
                throw SnowballRException.EmailException.TemplateCompilationFailed(template.templateFileName, e)
            }
        }
    }

    /**
     * Retrieves a compiled template for the specified email template type.
     *
     * @param emailTemplate The enum representing the desired email template.
     * @return THe compiled `Template` object.
     */
    @Suppress("MapGetWithNotNullAssertionOperator", "UnsafeCallOnNullableType")
    fun getTemplate(emailTemplate: EmailTemplate): Template = templates[emailTemplate]!!
}
