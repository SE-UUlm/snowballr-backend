package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ProjectService
import se.uulm.snowballr.backend.service.withUser
import snowballr.ProjectOuterClass
import kotlin.test.assertEquals

/**
 * Base test class for the [ProjectService].
 */
sealed class ProjectServiceTest : BaseServiceTest {
    val projectRepoMock = mockk<IProjectTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val invitationTokenRepoMock = mockk<IInvitationTokenTableRepo>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()
    val fetcherManagerMock = mockk<IFetcherManager>()

    private val allMocks = arrayOf(
        projectRepoMock,
        userRepoMock,
        projectMemberRepoMock,
        projectPaperRepoMock,
        criterionRepoMock,
        invitationTokenRepoMock,
        projectAccessCheckerMock,
        fetcherManagerMock,
    )

    val service = ProjectService(
        repo = projectRepoMock,
        userRepo = userRepoMock,
        projectMemberRepo = projectMemberRepoMock,
        projectPaperRepo = projectPaperRepoMock,
        criterionRepo = criterionRepoMock,
        invitationTokenRepo = invitationTokenRepoMock,
        accessChecker = projectAccessCheckerMock,
        fetcherManager = fetcherManagerMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }

    protected fun assertProjectEquality(expected: Project, actual: ProjectOuterClass.Project) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.status.toGrpc(), actual.status)
        assertEquals(expected.currentStage, actual.currentStage)
        assertEquals(expected.maxStage, actual.maxStage)
        assertEquals(expected.similarityThreshold, actual.settings.similarityThreshold)
        assertEquals(expected.snowballingType.toGrpc(), actual.settings.snowballingType)
        assertEquals(expected.reviewMaybeAllowed, actual.settings.reviewMaybeAllowed)
        assertEquals(expected.reviewDecisionMatrix, actual.settings.decisionMatrix)
        assertEquals(
            expected.fetchers,
            actual.settings.fetchersMap.mapValues { options -> options.value.optionsMap.mapValues { it.toString() } },
        )
    }
}
