package se.uulm.snowballr.backend.model.exception.internal.email

import se.uulm.snowballr.backend.model.exception.internal.EmailException
import java.io.IOException

/**
 * Thrown when an email template file cannot be found or compiled during application startup.
 * This is a fatal startup error.
 *
 * @param templateFileName The name of the file that failed to compile.
 * @param cause The original [IOException] from the template engine.
 */
class TemplateCompilationFailedException(
    templateFileName: String,
    cause: IOException,
) : EmailException("Failed to compile email template '$templateFileName'.", cause)
