package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.FetcherService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [FetcherService].
 */
sealed class FetcherServiceTest : BaseServiceTest() {
    val fetcherManagerMock = mockk<IFetcherManager>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()
    val paperRepoMock = mockk<IPaperTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()

    private val allMocks: Array<Any> = arrayOf(
        fetcherManagerMock,
        projectRepoMock,
        userRepoMock,
        projectAccessCheckerMock,
        paperRepoMock,
        projectPaperRepoMock,
    )

    val service = FetcherService(
        fetcherManager = fetcherManagerMock,
        projectRepo = projectRepoMock,
        userRepo = userRepoMock,
        projectAccessChecker = projectAccessCheckerMock,
        paperRepo = paperRepoMock,
        projectPaperRepo = projectPaperRepoMock,
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
