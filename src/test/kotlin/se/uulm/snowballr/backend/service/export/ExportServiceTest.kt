package se.uulm.snowballr.backend.service.export

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ExportService
import se.uulm.snowballr.backend.service.IExportService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [ExportService].
 */
sealed class ExportServiceTest : BaseServiceTest() {
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

    val service: IExportService by inject()

    private val module = module {
        single { projectRepoMock }
        single { projectMemberRepoMock }
        single { projectPaperRepoMock }
        single { reviewRepoMock }
        single { criterionRepoMock }
        single { userRepoMock }
        single { projectAccessCheckerMock }

        singleOf(::ExportService) { bind<IExportService>() }
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
