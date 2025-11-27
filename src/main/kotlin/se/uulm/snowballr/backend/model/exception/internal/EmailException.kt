package se.uulm.snowballr.backend.model.exception.internal

import se.uulm.snowballr.backend.mail.EmailManager
import se.uulm.snowballr.backend.model.exception.InternalException

/**
 * Represents an exception that occurs within the [EmailManager].
 *
 * @param message The message describing the email-related error.
 * @param cause The cause of the exception, which can be another exception, or null.
 */
open class EmailException protected constructor(
    message: String,
    cause: Throwable,
) : InternalException(message, cause)
