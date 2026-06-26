package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.ICriterionAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.CriterionService
import se.uulm.snowballr.backend.service.withUser
import snowballr.CriterionOuterClass
import kotlin.test.assertEquals

/**
 * Base test class for the [CriterionService].
 */
sealed class CriterionServiceTest : BaseServiceTest {
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

    val service = CriterionService(
        repo = criterionRepoMock,
        userRepo = userRepoMock,
        accessChecker = criterionAccessCheckerMock,
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

    protected fun assertCriterionEquality(expected: Criterion, actual: CriterionOuterClass.Criterion) {
        assertEquals(expected.tag, actual.tag)
        assertEquals(expected.name, actual.name)
        assertEquals(expected.description, actual.description)
        assertEquals(expected.category.toGrpc(), actual.category)
    }
}
