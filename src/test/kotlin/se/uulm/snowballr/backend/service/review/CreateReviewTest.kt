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
import se.uulm.snowballr.backend.DataBuilder.createExampleReviewDecisionMatrix
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateReviewException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix.Pattern
import snowballr.ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry
import snowballr.ReviewOuterClass
import snowballr.ReviewOuterClass.ReviewDecision
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateReviewTest : MainServiceTest() {
    private val userId = UUID.randomUUID()
    private val project = DataBuilder.createExampleProject(reviewDecisionMatrix = createExampleReviewDecisionMatrix())
    private val projectPaperId = UUID.randomUUID()
    private val decision = ReviewDecision.REVIEW_DECISION_ACCEPTED
    private val defaultCriterion = UUID.randomUUID()
    private val selectedCriteriaIds = listOf<UUID>(defaultCriterion)

    private val validCreateReviewRequest: ReviewOuterClass.Review.Create.Builder =
        ReviewOuterClass.Review.Create.newBuilder()
            .setProjectPaperId(projectPaperId.toString())
            .setDecision(decision)
            .addAllSelectedCriteriaIds(selectedCriteriaIds.map(UUID::toString))

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(projectRepoMock::getProjectById),
    )

    @Suppress("LongParameterList", "ReturnCount", "LongMethod")
    private fun mockCreateReview(
        useAdminUser: Boolean = true,
        project: Project = this.project,
        initialPaperDecision: PaperDecision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        updatedPaperDecision: PaperDecision = PaperDecision.PAPER_DECISION_IN_REVIEW,
        existingReviews: List<Review> = emptyList(),
        stopBefore: KFunction<*>? = null,
        failAt: KFunction<*>? = null,
    ) {
        val currentUser = DataBuilder.createExampleUser(
            id = userId,
            role = if (useAdminUser) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            decision = initialPaperDecision,
        )
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

        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        if (stopBefore == projectMemberRepoMock::getProjectMembers) {
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (useAdminUser) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (stopBefore == projectRepoMock::getProjectById) {
            return
        } else if (failAt == projectRepoMock::getProjectById) {
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        if (stopBefore == reviewRepoMock::getAllReviewsForProjectPaper) {
            return
        }
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns existingReviews
        if (stopBefore == reviewRepoMock::createReview) {
            return
        }
        coEvery {
            reviewRepoMock.createReview(validCreateReviewRequest.build(), currentUser.id)
        } returns review
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds
        coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns emptyList()
        coEvery {
            projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, updatedPaperDecision)
        } returns Unit
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a repo call fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockCreateReview(failAt = failAt)

        assertThrows<TestSpecificException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When a server admin creates a review, then no exception is thrown`() = runTest {
        mockCreateReview(useAdminUser = true)

        assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
    }

    @Test
    fun `When a project member creates a review, then no exception is thrown`() = runTest {
        mockCreateReview(useAdminUser = false)

        assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
    }

    @Test
    fun `When a non project member creates a review, then an UnauthorizedException is thrown`() = runTest {
        mockCreateReview(useAdminUser = false, stopBefore = projectMemberRepoMock::getProjectMembers)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
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
        val project = DataBuilder.createExampleProject(status = ProjectStatus.valueOf(statusName))
        mockCreateReview(project = project, stopBefore = projectRepoMock::getProjectById)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<FailedPreconditionException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When the user already reviewed the project paper, then a DuplicateReviewException is thrown`() = runTest {
        val firstReview = DataBuilder.createExampleReview(
            projectPaperId = projectPaperId,
            decision = decision,
            userId = userId,
        )
        mockCreateReview(stopBefore = reviewRepoMock::getAllReviewsForProjectPaper)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns listOf(firstReview)

        assertThrows<DuplicateReviewException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When the project paper is already finally decided, then a FailedPreconditionException is thrown`() = runTest {
        mockCreateReview(
            initialPaperDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
            stopBefore = reviewRepoMock::createReview,
        )

        assertThrows<FailedPreconditionException> { mainService.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When another user reviewed the project paper but not finally decided it, then no exception is thrown and the paper decision is updated accordingly to the review decision matrix`() =
        runTest {
            val reviewByAnotherUser = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = decision,
                userId = UUID.randomUUID(),
            )
            mockCreateReview(
                existingReviews = listOf(reviewByAnotherUser),
                initialPaperDecision = PaperDecision.PAPER_DECISION_IN_REVIEW,
                updatedPaperDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
            )

            assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
        }

    @Test
    fun `When the paper is not finally decided after the required number of reviews, then the paper is only accepted if the next review is an acceptance`() =
        runTest {
            val firstReview = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_MAYBE,
                userId = UUID.randomUUID(),
            )
            val secondReview = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_MAYBE,
                userId = UUID.randomUUID(),
            )
            mockCreateReview(
                existingReviews = listOf(firstReview, secondReview),
                initialPaperDecision = PaperDecision.PAPER_DECISION_IN_REVIEW,
                updatedPaperDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
            )

            assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
        }

    @Test
    fun `When no matching pattern could be found in the decision matrix, then the default paper decision is PAPER_DECISION_IN_REVIEW`() =
        runTest {
            val declinePattern = Pattern.newBuilder()
                .addEntries(Entry.newBuilder().setReviewDecision(ReviewDecision.REVIEW_DECISION_DECLINED).setCount(1))
                .setDecision(PaperDecision.PAPER_DECISION_DECLINED)
                .build()
            val project = DataBuilder.createExampleProject(
                reviewDecisionMatrix = createExampleReviewDecisionMatrix(
                    numberOfReviewers = 1,
                    pattern = listOf(declinePattern),
                ),
            )
            mockCreateReview(project = project)

            assertDoesNotThrow { mainService.createReview(validCreateReviewRequest.build()) }
            coVerify(exactly = 1) {
                projectPaperRepoMock.updateProjectPaperDecision(
                    projectPaperId,
                    PaperDecision.PAPER_DECISION_IN_REVIEW,
                )
            }
        }

    @Test
    fun `When a declining review decision is justified with a hard exclusion criterion, then the paper decision is instantly PAPER_DECISION_DECLINED`() =
        runTest {
            val exclusionCriterion = DataBuilder.createExampleProjectCriterion(
                category = CriterionCategory.CRITERION_CATEGORY_EXCLUSION,
            )
            val hardExclusionCriterion = DataBuilder.createExampleProjectCriterion(
                category = CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION,
            )
            val review = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_DECLINED,
                userId = userId,
            )
            val createReviewRequest = validCreateReviewRequest
                .setDecision(ReviewDecision.REVIEW_DECISION_DECLINED)
                .addAllSelectedCriteriaIds(
                    listOf(exclusionCriterion.id.toString(), hardExclusionCriterion.id.toString()),
                )
                .build()

            mockCreateReview(stopBefore = reviewRepoMock::createReview)
            coEvery { reviewRepoMock.createReview(createReviewRequest, userId) } returns review
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
            } returns listOf(defaultCriterion, exclusionCriterion.id, hardExclusionCriterion.id)
            coEvery {
                criterionRepoMock.getAllProjectCriteria(project.id)
            } returns listOf(exclusionCriterion, hardExclusionCriterion)
            coEvery {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
            } returns Unit

            assertDoesNotThrow { mainService.createReview(createReviewRequest) }
        }
}
