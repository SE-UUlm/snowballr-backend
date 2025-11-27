package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.model.exception.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

/**
 * Fetches the current user using the [userRepo] and executes the [block] with the current user as parameter.
 *
 * @throws NotFoundException if the current user does not exist.
 */
suspend fun <T> withUser(userRepo: IUserTableRepo, block: suspend (User) -> T): T {
    val user = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
    return block(user)
}

/**
 * Populates the given [Paper] with its backward references and converts it to a [GrpcPaper].
 */
suspend fun Paper.toGrpcPaperWithAuthorsAndBackwardReferences(citationRepo: ICitationTableRepo): GrpcPaper {
    val backwardReferences = citationRepo.getBackwardsReferencedPaperIdsOfPaperById(id).map(UUID::toString)
    return toGrpcPaper(backwardReferences)
}
