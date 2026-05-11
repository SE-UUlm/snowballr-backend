package se.uulm.snowballr.backend.model.exception.invalidargument

import se.uulm.snowballr.backend.model.exception.InvalidArgumentException

/**
 * Represents an exception that occurs when an invalid old password was provided for changing the password.
 */
class InvalidOldPasswordException : InvalidArgumentException("The provided old password is incorrect.")
