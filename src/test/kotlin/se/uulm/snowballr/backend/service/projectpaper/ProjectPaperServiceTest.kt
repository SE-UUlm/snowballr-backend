package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IProjectPaperAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ProjectPaperService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for [ProjectPaperService].
 */
sealed class ProjectPaperServiceTest : BaseServiceTest() {
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val paperRepoMock = mockk<IPaperTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()
    val reviewRepoMock = mockk<IReviewTableRepo>()
    val reviewHasCriterionRepoMock = mockk<IReviewHasCriterionTableRepo>()
    val projectPaperAccessCheckerMock = mockk<IProjectPaperAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        projectPaperRepoMock,
        userRepoMock,
        projectRepoMock,
        paperRepoMock,
        citationRepoMock,
        reviewRepoMock,
        reviewHasCriterionRepoMock,
        projectPaperAccessCheckerMock,
        projectAccessCheckerMock,
    )

    val service = ProjectPaperService(
        repo = projectPaperRepoMock,
        userRepo = userRepoMock,
        paperRepo = paperRepoMock,
        projectRepo = projectRepoMock,
        citationTableRepo = citationRepoMock,
        reviewTableRepo = reviewRepoMock,
        reviewHasCriterionTableRepo = reviewHasCriterionRepoMock,
        accessChecker = projectPaperAccessCheckerMock,
        projectAccessChecker = projectAccessCheckerMock,
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
