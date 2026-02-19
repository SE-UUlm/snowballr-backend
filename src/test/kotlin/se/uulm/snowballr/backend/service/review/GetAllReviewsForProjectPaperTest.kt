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
class GetAllReviewsForProjectPaperTest : MainServiceTest() {
    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
    )

    @Suppress("ReturnCount", "LongMethod")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean, projectPaperId: UUID) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id)
        val review = DataBuilder.createExampleReview(userId = currentUser.id)
        val selectedCriteriaIds = listOf<UUID>(UUID.randomUUID())

        mockCurrentUser(currentUser)

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaper.id)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)

        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns !isUserAdmin
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        val projectPaperId = UUID.randomUUID()

        mockHappyPathUntil(failAt, true, projectPaperId)

        assertThrows<TestSpecificException> {
            mainService.getAllReviewsForProjectPaper(projectPaperId)
        }
    }

    @Test
    fun `When a server admin retrieves all reviews for a project paper, then no exception is thrown`() = runTest {
        val projectPaperId = UUID.randomUUID()

        mockHappyPathUntil(null, true, projectPaperId)

        assertDoesNotThrow { mainService.getAllReviewsForProjectPaper(projectPaperId) }
    }

    @Test
    fun `When a project member retrieves all reviews for a project paper, then no exception is thrown`() = runTest {
        val projectPaperId = UUID.randomUUID()

        mockHappyPathUntil(null, false, projectPaperId)

        assertDoesNotThrow { mainService.getAllReviewsForProjectPaper(projectPaperId) }
    }

    @Test
    fun `When a non project member retrieves all reviews for a project paper, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id)

            mockCurrentUser(currentUser)
            coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns false

            assertThrows<UnauthorizedException> { mainService.getAllReviewsForProjectPaper(projectPaper.id) }
        }
}
