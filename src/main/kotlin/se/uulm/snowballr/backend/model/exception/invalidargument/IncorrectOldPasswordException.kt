package se.uulm.snowballr.backend.model.exception.invalidargument

import se.uulm.snowballr.backend.model.exception.InvalidArgumentException

/**
 * Represents an exception that occurs when an incorrect old password was provided for changing the password.
 */
class IncorrectOldPasswordException : InvalidArgumentException("The provided old password is incorrect.")
