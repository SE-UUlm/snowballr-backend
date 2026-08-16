package se.uulm.snowballr.backend.model.email

/**
 * Defines the available email templates, encapsulating both the template file name and the default subject for the email.
 *
 * @property templateFileName The name of the `.hbs` file in `/resources/templates` (without the extension).
 */
enum class EmailTemplate(val templateFileName: String) {
    EMAIL_VERIFICATION(templateFileName = "email-verification"),
    ACCEPT_PROJECT_INVITATION(templateFileName = "accept-project-invitation"),
}
