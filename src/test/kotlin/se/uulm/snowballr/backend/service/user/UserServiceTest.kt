package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IUserAccessChecker
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserSettings
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.UserService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [UserService].
 */
sealed class UserServiceTest : BaseServiceTest {
    val userRepoMock = mockk<IUserTableRepo>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val verificationTokenRepoMock = mockk<IVerificationTokenTableRepo>()
    val emailManagerMock = mockk<IEmailManager>()
    val envReaderMock = mockk<EnvReader>()
    val userAccessCheckerMock = mockk<IUserAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        userRepoMock,
        projectRepoMock,
        criterionRepoMock,
        verificationTokenRepoMock,
        emailManagerMock,
        envReaderMock,
        userAccessCheckerMock,
        projectAccessCheckerMock,
    )

    val service = UserService(
        userRepo = userRepoMock,
        projectRepo = projectRepoMock,
        criterionRepo = criterionRepoMock,
        verificationTokenRepo = verificationTokenRepoMock,
        emailManager = emailManagerMock,
        envReader = envReaderMock,
        accessChecker = userAccessCheckerMock,
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

    protected fun assertUserSettingsEquality(expected: UserSettings, actual: UserSettings) {
        assertEquals(expected.areHotkeysShown, actual.areHotkeysShown)
        assertEquals(expected.isReviewModeEnabled, actual.isReviewModeEnabled)
        assertEquals(expected.criteriaIds, actual.criteriaIds)
        assertEquals(expected.similarityThreshold, actual.similarityThreshold)
        assertEquals(expected.decisionMatrix, actual.decisionMatrix)
        assertEquals(expected.fetchers, actual.fetchers)
        assertEquals(expected.snowballingType, actual.snowballingType)
        assertEquals(expected.reviewMaybeAllowed, actual.reviewMaybeAllowed)
    }
}
