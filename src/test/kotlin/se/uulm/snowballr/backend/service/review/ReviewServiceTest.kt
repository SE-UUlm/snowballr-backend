package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IReviewAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.IReviewService
import se.uulm.snowballr.backend.service.ReviewService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [ReviewService].
 */
sealed class ReviewServiceTest : BaseServiceTest() {
    val reviewRepoMock = mockk<IReviewTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val reviewHasCriterionRepoMock = mockk<IReviewHasCriterionTableRepo>()
    val reviewAccessCheckerMock = mockk<IReviewAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        reviewRepoMock,
        userRepoMock,
        projectPaperRepoMock,
        projectRepoMock,
        criterionRepoMock,
        reviewHasCriterionRepoMock,
        reviewAccessCheckerMock,
        projectAccessCheckerMock,
    )

    val service: IReviewService by inject()

    private val module = module {
        single { reviewRepoMock }
        single { userRepoMock }
        single { projectPaperRepoMock }
        single { projectRepoMock }
        single { criterionRepoMock }
        single { reviewHasCriterionRepoMock }
        single { reviewAccessCheckerMock }
        single { projectAccessCheckerMock }

        singleOf(::ReviewService) { bind<IReviewService>() }
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
