package se.uulm.snowballr.backend.scheduler

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import kotlin.getValue

/**
 * Service interface for maintaining tokens in the system.
 *
 * Implementations may provide various maintenance operations such as
 * cleaning up expired tokens, auditing, or other token-related tasks.
 */
interface ITokenMaintenanceService {
    /**
     * Deletes all tokens whose expiry timestamp is less than or equal to the current time.
     */
    suspend fun deleteExpiredTokens()
}

/**
 * Default implementation of [ITokenMaintenanceService].
 *
 * Provides basic token maintenance operations using the configured database.
 */
class TokenMaintenanceService : ITokenMaintenanceService, KoinComponent {
    private val verificationTokenRepo: IVerificationTokenTableRepo by inject()
    private val invitationTokenRepo: IInvitationTokenTableRepo by inject()

    override suspend fun deleteExpiredTokens() {
        verificationTokenRepo.deleteExpiredVerificationTokens()
        invitationTokenRepo.deleteExpiredInvitationTokens()
    }
}
