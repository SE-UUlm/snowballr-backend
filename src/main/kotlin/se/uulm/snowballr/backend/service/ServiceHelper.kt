package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo

/**
 * Fetches the current user using the [userRepo] and executes the [block] with the current user as parameter.
 *
 * @throws NotFoundException if the current user does not exist.
 */
suspend fun <T> withUser(userRepo: IUserTableRepo, block: suspend (User) -> T): T {
    val user = userRepo.getUserById(RequestContext.current().requireUserId()).getOrThrow()
    return block(user)
}

/**
 * Populates the given [Paper] with its backward references and converts it to a [PaperResponse].
 */
suspend fun Paper.toPaperResponse(citationRepo: ICitationTableRepo): PaperResponse {
    val backwardReferencedIds = citationRepo.getBackwardsReferencedPaperIdsOfPaperById(id)
    return PaperResponse.fromPaper(this, backwardReferencedIds)
}
