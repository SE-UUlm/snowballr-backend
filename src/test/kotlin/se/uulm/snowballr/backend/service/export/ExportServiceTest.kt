package se.uulm.snowballr.backend.service.export

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ExportService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [ExportService].
 */
sealed class ExportServiceTest : BaseServiceTest {
    val projectRepoMock = mockk<IProjectTableRepo>()
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val reviewRepoMock = mockk<IReviewTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        projectRepoMock,
        projectMemberRepoMock,
        projectPaperRepoMock,
        reviewRepoMock,
        criterionRepoMock,
        userRepoMock,
        projectAccessCheckerMock,
    )

    val service = ExportService(
        projectRepo = projectRepoMock,
        projectMemberRepo = projectMemberRepoMock,
        projectPaperRepo = projectPaperRepoMock,
        reviewRepo = reviewRepoMock,
        projectAccessChecker = projectAccessCheckerMock,
        userRepo = userRepoMock,
        criterionRepo = criterionRepoMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
