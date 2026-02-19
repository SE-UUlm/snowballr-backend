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
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReviewByIdTest : MainServiceTest() {
    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(reviewRepoMock::getReviewById),
    )

    @Suppress("ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean, reviewId: UUID) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val review = DataBuilder.createExampleReview(id = reviewId, userId = currentUser.id)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)
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
        coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns !isUserAdmin
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        val reviewId = UUID.randomUUID()

        mockHappyPathUntil(failAt, true, reviewId)

        assertThrows<TestSpecificException> {
            mainService.getReviewById(reviewId)
        }
    }

    @Test
    fun `When a server admin retrieves the review, then no exception is thrown`() = runTest {
        val reviewId = UUID.randomUUID()

        mockHappyPathUntil(null, true, reviewId)

        assertDoesNotThrow { mainService.getReviewById(reviewId) }
    }

    @Test
    fun `When a project member retrieves the review, then no exception is thrown`() = runTest {
        val reviewId = UUID.randomUUID()

        mockHappyPathUntil(null, false, reviewId)

        assertDoesNotThrow { mainService.getReviewById(reviewId) }
    }

    @Test
    fun `When a non project member retrieves the review, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val review = DataBuilder.createExampleReview(userId = currentUser.id)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)

        mockCurrentUser(currentUser)
        coEvery { reviewRepoMock.getReviewById(review.id) } returns Result.success(review)
        coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns false

        assertThrows<UnauthorizedException> { mainService.getReviewById(review.id) }
    }
}
