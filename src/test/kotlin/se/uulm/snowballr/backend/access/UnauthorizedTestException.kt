package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import java.util.UUID

class UnauthorizedTestException(
    currentUserId: UUID = UUID.randomUUID(),
    accessType: AccessType = AccessType.READ,
    accessedEntityMessage: String = "Test exception for access type $accessType",
) : UnauthorizedException(currentUserId, accessType, accessedEntityMessage)
