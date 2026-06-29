package se.uulm.snowballr.backend.service.authentication

import io.mockk.coEvery
import io.mockk.mockk
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.service.AuthenticationService
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [AuthenticationService].
 */
sealed class AuthenticationServiceTest : BaseServiceTest {
    val userRepoMock = mockk<IUserTableRepo>()
    val verificationTokenRepoMock = mockk<IVerificationTokenTableRepo>()
    val jwtManagerMock = mockk<IJwtManager>()

    private val allMocks = arrayOf(userRepoMock, verificationTokenRepoMock, jwtManagerMock)

    val service = AuthenticationService(
        repo = userRepoMock,
        verificationTokenRepo = verificationTokenRepoMock,
        jwtManager = jwtManagerMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        RequestContext.current().userId = currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
