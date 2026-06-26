package se.uulm.snowballr.backend.service.review

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.DataBuilder.createExampleReviewDecisionMatrix
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateReviewException
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.ReviewDecisionMatrix.Pattern
import snowballr.ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry
import snowballr.ReviewOuterClass
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import snowballr.ProjectOuterClass.Project as GrpcProject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateReviewTest : ReviewServiceTest() {
    private val userId = UUID.randomUUID()
    private val project = DataBuilder.createExampleProject(reviewDecisionMatrix = createExampleReviewDecisionMatrix())
    private val projectPaperId = UUID.randomUUID()
    private val decision = ReviewDecision.REVIEW_DECISION_ACCEPTED
    private val defaultCriterion = UUID.randomUUID()
    private val selectedCriteriaIds = listOf<UUID>(defaultCriterion)

    private val validCreateReviewRequest: ReviewOuterClass.Review.Create.Builder =
        ReviewOuterClass.Review.Create.newBuilder()
            .setProjectPaperId(projectPaperId.toString())
            .setDecision(decision.toGrpc())
            .addAllSelectedCriteriaIds(selectedCriteriaIds.map(UUID::toString))

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(reviewAccessCheckerMock::isAllowedToCreateReview),
        Arguments.of(projectRepoMock::getProjectById),
    )

    fun getUpdateProjectStatusRequest(projectId: UUID): GrpcProject.Update = GrpcProject.Update.newBuilder()
        .setProject(
            GrpcProject.newBuilder()
                .setId(projectId.toString())
                .setStatus(ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED.toGrpc())
                .build(),
        )
        .setMask(FieldMaskUtil.fromString("project.status"))
        .build()

    @Suppress("LongParameterList", "ReturnCount", "LongMethod")
    private fun mockCreateReview(
        project: Project = this.project,
        initialPaperDecision: PaperDecision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        updatedPaperDecision: PaperDecision = PaperDecision.PAPER_DECISION_IN_REVIEW,
        existingReviews: List<Review> = emptyList(),
        stopBefore: KFunction<*>? = null,
        failAt: KFunction<*>? = null,
    ) {
        val currentUser = DataBuilder.createExampleUser(id = userId)
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
        val projectResult = Result.success(project)

        mockCurrentUser(currentUser)

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaperId)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaperId) } returns Result.success(projectPaper)

        if (stopBefore == projectRepoMock::getProjectById) {
            return
        } else if (failAt == projectRepoMock::getProjectById) {
            val result = Result.failure<Project>(TestSpecificException())
            coEvery { projectRepoMock.getProjectById(project.id) } returns result
            coJustRun { reviewAccessCheckerMock.isAllowedToCreateReview(currentUser, project.id, result) }
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult

        if (stopBefore == reviewAccessCheckerMock::isAllowedToCreateReview) {
            return
        } else if (failAt == reviewAccessCheckerMock::isAllowedToCreateReview) {
            coEvery {
                reviewAccessCheckerMock.isAllowedToCreateReview(currentUser, project.id, projectResult)
            } throws TestSpecificException()
            return
        }
        coJustRun { reviewAccessCheckerMock.isAllowedToCreateReview(currentUser, project.id, projectResult) }

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
        coJustRun { projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, updatedPaperDecision) }
        if (project.status != ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED) {
            coJustRun { projectRepoMock.updateProject(getUpdateProjectStatusRequest(project.id)) }
        }
        if (updatedPaperDecision == PaperDecision.PAPER_DECISION_ACCEPTED) {
            coJustRun { fetcherOrchestratorMock.enqueue(FetcherEnqueueJob(projectPaper, currentUser.id)) }
        }
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a repo call fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockCreateReview(failAt = failAt)

        assertThrows<TestSpecificException> { service.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When a user creates a review and has access, then the created review has the correct values`() = runTest {
        mockCreateReview()

        val review = service.createReview(validCreateReviewRequest.build())

        assertEquals(userId.toString(), review.userId)
        assertEquals(decision.toGrpc(), review.decision)
        assertEquals(selectedCriteriaIds.map { it.toString() }, review.selectedCriteriaIdsList)

        coVerify(exactly = 1) {
            projectRepoMock.updateProject(getUpdateProjectStatusRequest(project.id))
        }
    }

    @Test
    fun `When a user already reviewed the project paper, then a DuplicateReviewException is thrown`() = runTest {
        val firstReview = DataBuilder.createExampleReview(
            projectPaperId = projectPaperId,
            decision = decision,
            userId = userId,
        )
        mockCreateReview(stopBefore = reviewRepoMock::getAllReviewsForProjectPaper)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaperId) } returns listOf(firstReview)

        assertThrows<DuplicateReviewException> { service.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When the project paper is already finally decided, then a FailedPreconditionException is thrown`() = runTest {
        mockCreateReview(
            initialPaperDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
            stopBefore = reviewRepoMock::createReview,
        )

        assertThrows<FailedPreconditionException> { service.createReview(validCreateReviewRequest.build()) }
        coVerify(exactly = 0) { reviewRepoMock.createReview(any(), any()) }
    }

    @Test
    fun `When another user reviewed the project paper but not finally decided it, then the paper decision is updated accordingly to the review decision matrix`() =
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

            service.createReview(validCreateReviewRequest.build())

            coVerify(exactly = 1) {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_ACCEPTED)
            }
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

            service.createReview(validCreateReviewRequest.build())

            coVerify(exactly = 1) {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_ACCEPTED)
            }
        }

    @Test
    fun `When no matching pattern could be found in the decision matrix, then the default paper decision is PAPER_DECISION_IN_REVIEW`() =
        runTest {
            val declinePattern = Pattern.newBuilder()
                .addEntries(
                    Entry.newBuilder()
                        .setReviewDecision(ReviewDecision.REVIEW_DECISION_DECLINED.toGrpc())
                        .setCount(1),
                )
                .setDecision(PaperDecision.PAPER_DECISION_DECLINED)
                .build()
            val project = DataBuilder.createExampleProject(
                reviewDecisionMatrix = createExampleReviewDecisionMatrix(
                    numberOfReviewers = 1,
                    pattern = listOf(declinePattern),
                ),
            )
            mockCreateReview(project = project)

            service.createReview(validCreateReviewRequest.build())
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
                .setDecision(ReviewDecision.REVIEW_DECISION_DECLINED.toGrpc())
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
            coJustRun {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
            }
            coJustRun { projectRepoMock.updateProject(getUpdateProjectStatusRequest(project.id)) }

            service.createReview(createReviewRequest)

            coVerify(exactly = 1) {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
            }
        }

    @Test
    fun `When required review count is exceeded and the latest review is REVIEW_DECISION_DECLINED, then final paper decision is REVIEW_DECISION_DECLINED`() =
        runTest {
            val project = DataBuilder.createExampleProject(
                reviewDecisionMatrix = createExampleReviewDecisionMatrix(numberOfReviewers = 2),
            )
            val firstReview = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_MAYBE,
                userId = UUID.randomUUID(),
            )
            val secondReview = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_ACCEPTED,
                userId = UUID.randomUUID(),
            )
            val declineReview = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_DECLINED,
                userId = userId,
            )
            val createReviewRequest = ReviewOuterClass.Review.Create.newBuilder()
                .setProjectPaperId(projectPaperId.toString())
                .setDecision(ReviewDecision.REVIEW_DECISION_DECLINED.toGrpc())
                .addAllSelectedCriteriaIds(selectedCriteriaIds.map(UUID::toString))
                .build()

            mockCreateReview(
                project = project,
                existingReviews = listOf(firstReview, secondReview),
                stopBefore = reviewRepoMock::createReview,
            )
            coEvery { reviewRepoMock.createReview(createReviewRequest, userId) } returns declineReview
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(declineReview.id)
            } returns selectedCriteriaIds
            coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns emptyList()
            coJustRun {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
            }
            coJustRun { projectRepoMock.updateProject(getUpdateProjectStatusRequest(project.id)) }

            service.createReview(createReviewRequest)
            coVerify(exactly = 1) {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
            }
        }

    @Test
    fun `When hard exclusion criterion is selected but review is REVIEW_DECISION_ACCEPTED, then normal decision matrix is used`() =
        runTest {
            val hardExclusionCriterion = DataBuilder.createExampleProjectCriterion(
                category = CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION,
            )
            val acceptedReview = DataBuilder.createExampleReview(
                projectPaperId = projectPaperId,
                decision = ReviewDecision.REVIEW_DECISION_ACCEPTED,
                userId = userId,
            )
            val createReviewRequest = ReviewOuterClass.Review.Create.newBuilder()
                .setProjectPaperId(projectPaperId.toString())
                .setDecision(ReviewDecision.REVIEW_DECISION_ACCEPTED.toGrpc())
                .addAllSelectedCriteriaIds(selectedCriteriaIds.map(UUID::toString))
                .addSelectedCriteriaIds(hardExclusionCriterion.id.toString())
                .build()

            mockCreateReview(stopBefore = reviewRepoMock::createReview)
            coEvery { reviewRepoMock.createReview(createReviewRequest, userId) } returns acceptedReview
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(acceptedReview.id)
            } returns listOf(defaultCriterion, hardExclusionCriterion.id)
            coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns listOf(hardExclusionCriterion)
            coJustRun {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_IN_REVIEW)
            }
            coJustRun { projectRepoMock.updateProject(getUpdateProjectStatusRequest(project.id)) }

            service.createReview(createReviewRequest)
            coVerify(exactly = 1) {
                projectPaperRepoMock.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_IN_REVIEW)
            }
        }

    @Test
    fun `When the project has already status ACTIVE_LOCKED, then the status is not updated again`() = runTest {
        val project = project.copy(status = ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)
        val createReviewRequest = ReviewOuterClass.Review.Create.newBuilder()
            .setProjectPaperId(projectPaperId.toString())
            .setDecision(ReviewDecision.REVIEW_DECISION_ACCEPTED.toGrpc())
            .addAllSelectedCriteriaIds(selectedCriteriaIds.map(UUID::toString))
            .build()

        mockCreateReview(project)

        service.createReview(createReviewRequest)

        coVerify(exactly = 0) {
            projectRepoMock.updateProject(any())
        }
    }
}
