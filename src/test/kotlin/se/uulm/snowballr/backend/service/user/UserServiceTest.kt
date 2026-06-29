package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IUserAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
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
import snowballr.UserSettingsOuterClass
import kotlin.test.assertEquals

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
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }

    protected fun assertUserSettingsEquality(expected: UserSettings, actual: UserSettingsOuterClass.UserSettings) {
        assertEquals(expected.areHotkeysShown, actual.showHotkeys)
        assertEquals(expected.isReviewModeEnabled, actual.reviewMode)
        assertEquals(expected.criteriaIds.map { it.toString() }, actual.defaultCriteria.criteriaList.map { it.id })
        assertEquals(expected.similarityThreshold, actual.defaultProjectSettings.similarityThreshold)
        assertEquals(expected.decisionMatrix.toGrpc(), actual.defaultProjectSettings.decisionMatrix)
        assertEquals(
            expected.fetchers,
            actual.defaultProjectSettings.fetchersMap.mapValues { options -> options.value.optionsMap },
        )
        assertEquals(expected.snowballingType.toGrpc(), actual.defaultProjectSettings.snowballingType)
        assertEquals(expected.reviewMaybeAllowed, actual.defaultProjectSettings.reviewMaybeAllowed)
    }
}
