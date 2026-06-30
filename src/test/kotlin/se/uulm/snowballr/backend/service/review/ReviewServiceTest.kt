package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IReviewAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.fetcher.IFetcherOrchestrator
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ReviewService
import se.uulm.snowballr.backend.service.withUser
import kotlin.test.assertEquals

/**
 * Base test class for the [ReviewService].
 */
sealed class ReviewServiceTest : BaseServiceTest {
    val reviewRepoMock = mockk<IReviewTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val reviewHasCriterionRepoMock = mockk<IReviewHasCriterionTableRepo>()
    val reviewAccessCheckerMock = mockk<IReviewAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()
    val fetcherOrchestratorMock = mockk<IFetcherOrchestrator>()

    private val allMocks = arrayOf(
        reviewRepoMock,
        userRepoMock,
        projectPaperRepoMock,
        projectRepoMock,
        criterionRepoMock,
        reviewHasCriterionRepoMock,
        reviewAccessCheckerMock,
        projectAccessCheckerMock,
        fetcherOrchestratorMock,
    )

    val service = ReviewService(
        repo = reviewRepoMock,
        userRepo = userRepoMock,
        projectPaperRepo = projectPaperRepoMock,
        projectRepo = projectRepoMock,
        criterionRepo = criterionRepoMock,
        reviewHasCriterionRepo = reviewHasCriterionRepoMock,
        accessChecker = reviewAccessCheckerMock,
        projectAccessChecker = projectAccessCheckerMock,
        fetcherOrchestrator = fetcherOrchestratorMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }

    protected fun assertReviewEquality(expected: Review, actual: ReviewResponse) {
        assertEquals(expected.userId, actual.userId)
        assertEquals(expected.decision, actual.decision)
    }
}
