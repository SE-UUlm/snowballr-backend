package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReviewByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestId.toString()).build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(reviewRepoMock::getReviewById),
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(projectRepoMock::getProjectById),
    )

    @Suppress("ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserOuterClass.UserRole.USER_ROLE_ADMIN
            } else {
                UserOuterClass.UserRole.USER_ROLE_DEFAULT
            },
        )
        val review = DataBuilder.createExampleReview(id = requestId, userId = currentUser.id)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val selectedCriteriaIds = listOf<UUID>(UUID.randomUUID())

        mockCurrentUser(currentUser)

        if (failAt == reviewRepoMock::getReviewById) {
            coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.success(review)

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery {
                projectPaperRepoMock.getProjectPaperById(review.projectPaperId)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } returns Result.success(projectPaper)

        if (failAt == projectRepoMock::getProjectById) {
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt, true)

        assertThrows<TestSpecificException> {
            mainService.getReviewById(getExampleRequest())
        }
    }

    @Test
    fun `When a server admin retrieves the review, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)

        assertDoesNotThrow { mainService.getReviewById(getExampleRequest()) }
    }

    @Test
    fun `When a project member retrieves the review, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)

        assertDoesNotThrow { mainService.getReviewById(getExampleRequest()) }
    }

    @Test
    fun `When a non project member retrieves the review, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val review = DataBuilder.createExampleReview(id = requestId, userId = currentUser.id)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)

        mockCurrentUser(currentUser)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.success(review)
        coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } returns Result.success(projectPaper)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getReviewById(getExampleRequest()) }
    }
}
