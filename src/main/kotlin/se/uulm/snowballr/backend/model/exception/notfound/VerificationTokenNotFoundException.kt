package se.uulm.snowballr.backend.model.exception.notfound

import se.uulm.snowballr.backend.model.exception.NotFoundException

/**
 * Represents an exception that occurs when a verification token could not be found.
 */
class VerificationTokenNotFoundException : NotFoundException("Verification token not found.")
