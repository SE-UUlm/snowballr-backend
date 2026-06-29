package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IProjectMemberAccessChecker
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ProjectMemberService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [ProjectMemberService].
 */
sealed class ProjectMemberServiceTest : BaseServiceTest {
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val invitationTokenRepoMock = mockk<IInvitationTokenTableRepo>()
    val projectMemberAccessCheckerMock = mockk<IProjectMemberAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        projectMemberRepoMock,
        projectRepoMock,
        userRepoMock,
        invitationTokenRepoMock,
        projectMemberAccessCheckerMock,
        projectAccessCheckerMock,
    )

    val service = ProjectMemberService(
        repo = projectMemberRepoMock,
        projectRepo = projectRepoMock,
        userRepo = userRepoMock,
        invitationTokenRepo = invitationTokenRepoMock,
        accessChecker = projectMemberAccessCheckerMock,
        projectAccessChecker = projectAccessCheckerMock,
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
