package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.service.AuthenticationService
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [AuthenticationService].
 */
sealed class AuthenticationServiceTest : BaseServiceTest() {
    val userRepoMock = mockk<IUserTableRepo>()
    val verificationTokenRepoMock = mockk<IVerificationTokenTableRepo>()
    val jwtManagerMock = mockk<IJwtManager>()

    val allMocks = arrayOf(userRepoMock, verificationTokenRepoMock, jwtManagerMock)

    val service: IAuthenticationService by inject()

    private val module = module {
        single { userRepoMock }
        single { verificationTokenRepoMock }
        single { jwtManagerMock }
    }

    override fun getModule(): Module = module

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
