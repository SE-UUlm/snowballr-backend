package se.uulm.snowballr.backend.model.exception.notfound

import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException

/**
 * Represents an exception that occurs when an invitation token could not be found.
 */
class InvitationTokenNotFoundException : NotFoundException("Invitation token not found.")
