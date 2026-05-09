package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.access.ICriterionAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.CriterionService
import se.uulm.snowballr.backend.service.ICriterionService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [CriterionService].
 */
sealed class CriterionServiceTest : BaseServiceTest() {
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val criterionAccessCheckerMock = mockk<ICriterionAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        criterionRepoMock,
        userRepoMock,
        criterionAccessCheckerMock,
        projectAccessCheckerMock,
    )

    val service: ICriterionService by inject()

    private val module = module {
        single { criterionRepoMock }
        single { userRepoMock }
        single { criterionAccessCheckerMock }
        single { projectAccessCheckerMock }

        singleOf(::CriterionService) { bind<ICriterionService>() }
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
