package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.ProjectService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [ProjectService].
 */
sealed class ProjectServiceTest : BaseServiceTest() {
    val projectRepoMock = mockk<IProjectTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val invitationTokenRepoMock = mockk<IInvitationTokenTableRepo>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    val allMocks = arrayOf(
        projectRepoMock,
        userRepoMock,
        projectMemberRepoMock,
        projectPaperRepoMock,
        criterionRepoMock,
        invitationTokenRepoMock,
        projectAccessCheckerMock,
    )

    val service: IProjectService by inject()

    private val module = module {
        single { projectRepoMock }
        single { userRepoMock }
        single { projectMemberRepoMock }
        single { projectPaperRepoMock }
        single { criterionRepoMock }
        single { invitationTokenRepoMock }
        single { projectAccessCheckerMock }
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
