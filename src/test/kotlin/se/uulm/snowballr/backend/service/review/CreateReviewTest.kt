package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateReviewException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ReviewOuterClass
import snowballr.ReviewOuterClass.ReviewDecision
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateReviewTest : MainServiceTest() {
    private val projectPaperId = UUID.randomUUID()
    private val decision = ReviewDecision.REVIEW_DECISION_ACCEPTED
    private val selectedCriteriaIds = listOf<UUID>(UUID.randomUUID())

    private val validCreateReviewRequest: ReviewOuterClass.Review.Create.Builder =
        ReviewOuterClass.Review.Create.newBuilder()
            .setProjectPaperId(projectPaperId.toString())
            .setDecision(decision)
            .addAllSelectedCriteriaIds(selectedCriteriaIds.map(UUID::toString))

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(projectRepoMock::getProjectById),
    )

    @Suppress("ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val review = DataBuilder.createExampleReview(
            projectPaperId = projectPaperId,
            decision = decision,
            userId = currentUser.id,
        )

        mockCurrentUser(currentUser)

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaperId)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)

        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (failAt == projectRepoMock::getProjectById) {
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns emptyList()
        coEvery {
            reviewRepoMock.createReview(validCreateReviewRequest.build(), currentUser.id)
        } returns review
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt, true)

        assertThrows<TestSpecificException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When a server admin creates a review, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)

        assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
    }

    @Test
    fun `When a project member creates a review, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)

        assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
    }

    @Test
    fun `When a non project member creates a review, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id)

        mockCurrentUser(currentUser)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When the user already reviewed the project paper, then a DuplicateReviewException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id)
        val review = DataBuilder.createExampleReview(
            projectPaperId = projectPaperId,
            decision = decision,
            userId = currentUser.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns listOf(review)

        assertThrows<DuplicateReviewException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When another user already reviewed the project paper but not finally decided it, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val anotherUser = DataBuilder.createExampleUser(
                email = "another.user@example.com",
                role = UserRole.USER_ROLE_DEFAULT,
            )
            val project = DataBuilder.createExampleProject()
            val projectPaper = DataBuilder.createExampleProjectPaper(
                id = projectPaperId,
                projectId = project.id,
                decision = PaperDecision.PAPER_DECISION_IN_REVIEW,
            )
            val reviewByAnotherUser = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = decision,
                userId = anotherUser.id,
            )
            val review = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = decision,
                userId = currentUser.id,
            )
            val projectMember1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
            val projectMember2 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = anotherUser.id)

            mockCurrentUser(currentUser)
            coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectMember1, projectMember2)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns listOf(reviewByAnotherUser)
            coEvery { reviewRepoMock.createReview(validCreateReviewRequest.build(), currentUser.id) } returns review
            coEvery { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) } returns emptyList()

            assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
        }

    @ParameterizedTest
    @CsvSource(
        value = [
            "PROJECT_STATUS_ARCHIVED",
            "PROJECT_STATUS_DELETED",
        ],
    )
    fun `When the project paper to review is in an inactive project, then a FailedPreconditionException is thrown`(
        statusName: String,
    ) = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(status = ProjectStatus.valueOf(statusName))
        val projectPaper = DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id)

        mockCurrentUser(currentUser)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<FailedPreconditionException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When the project paper is already finally decided, then a FailedPreconditionException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            decision = PaperDecision.PAPER_DECISION_ACCEPTED,
        )

        mockCurrentUser(currentUser)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns emptyList()

        assertThrows<FailedPreconditionException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }
}
