package se.uulm.snowballr.backend.model.email

/**
 * Defines the available email templates, encapsulating both the template file name and the default subject for the email.
 *
 * @property templateFileName The name of the `.hbs` file in `/resources/templates` (without the extension).
 * @property subject The default subject line for the email type.
 */
enum class EmailTemplate(val templateFileName: String, val subject: String) {
    EMAIL_VERIFICATION(
        templateFileName = "email-verification",
        subject = "Verify your SnowballR Account",
    ),
}
