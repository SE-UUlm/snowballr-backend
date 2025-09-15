package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IUserTableRepo

/**
 * Fetches the current user using the [userRepo] and executes the [block] with the current user as parameter.
 *
 * @throws NotFoundException if the current user does not exist.
 */
suspend fun <T> withUser(userRepo: IUserTableRepo, block: suspend (User) -> T): T {
    val user = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
    return block(user)
}
