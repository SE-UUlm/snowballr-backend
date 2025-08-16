package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
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
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReviewByIdTest : MainServiceTest() {
    private val reviewId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(reviewId.toString()).build()

    // Test data defined at class level to be accessible in both mock setup and verification
    private val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
    private val review = DataBuilder.createExampleReview(id = reviewId, userId = adminUser.id)
    private val project = DataBuilder.createExampleProject()
    private val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)
    private val selectedCriteriaIds = listOf(UUID.randomUUID())

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(GrpcContext::getUserIdFromContext),
        Arguments.of(userRepoMock::getUserById),
        Arguments.of(reviewRepoMock::getReviewById),
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(projectRepoMock::getProjectById),
        Arguments.of(projectMemberRepoMock::getProjectMembers),
        Arguments.of(reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById),
    )

    @Suppress("ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?) {
        if (failAt == GrpcContext::getUserIdFromContext) {
            every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
            return
        }
        every { GrpcContext.getUserIdFromContext() } returns adminUser.id

        if (failAt == userRepoMock::getUserById) {
            coEvery { userRepoMock.getUserById(adminUser.id) } throws TestSpecificException()
            return
        }
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser

        if (failAt == reviewRepoMock::getReviewById) {
            coEvery { reviewRepoMock.getReviewById(review.id) } throws TestSpecificException()
            return
        }
        coEvery { reviewRepoMock.getReviewById(review.id) } returns review

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } throws TestSpecificException()
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } returns projectPaper

        if (failAt == projectRepoMock::getProjectById) {
            coEvery { projectRepoMock.getProjectById(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns project

        if (failAt == projectMemberRepoMock::getProjectMembers) {
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        if (failAt == reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById) {
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then an exception is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt)
        assertThrows<TestSpecificException> {
            mainService.getReviewById(getExampleRequest())
        }

        // Verification logic for each failure point
        when (failAt) {
            GrpcContext::getUserIdFromContext -> {
                verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
                coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
            }

            userRepoMock::getUserById -> {
                verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
                coVerify(exactly = 1) { userRepoMock.getUserById(adminUser.id) }
                coVerify(exactly = 0) { reviewRepoMock.getReviewById(any()) }
            }

            reviewRepoMock::getReviewById -> {
                coVerify(exactly = 1) { userRepoMock.getUserById(adminUser.id) }
                coVerify(exactly = 1) { reviewRepoMock.getReviewById(reviewId) }
                coVerify(exactly = 0) { projectPaperRepoMock.getProjectPaperById(any()) }
            }

            projectPaperRepoMock::getProjectPaperById -> {
                coVerify(exactly = 1) { reviewRepoMock.getReviewById(reviewId) }
                coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) }
                coVerify(exactly = 0) { projectRepoMock.getProjectById(any()) }
            }

            projectRepoMock::getProjectById -> {
                coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) }
                coVerify(exactly = 1) { projectRepoMock.getProjectById(project.id) }
                coVerify(exactly = 0) { projectMemberRepoMock.getProjectMembers(any()) }
            }

            projectMemberRepoMock::getProjectMembers -> {
                coVerify(exactly = 1) { projectRepoMock.getProjectById(project.id) }
                coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
                coVerify(exactly = 0) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(any()) }
            }

            reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById -> {
                coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
                coVerify(exactly = 1) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) }
            }
        }
    }

    @Test
    fun `When a server admin retrieves the review, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null)
        assertDoesNotThrow { mainService.getReviewById(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(adminUser.id) }
        coVerify(exactly = 1) { reviewRepoMock.getReviewById(reviewId) }
        coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) }
        coVerify(exactly = 1) { projectRepoMock.getProjectById(project.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
        coVerify(exactly = 1) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) }
    }

    @Test
    fun `When a project member retrieves the review, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val review = DataBuilder.createExampleReview(id = reviewId, userId = currentUser.id)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val selectedCriteriaIds = listOf(UUID.randomUUID())

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { reviewRepoMock.getReviewById(review.id) } returns review
        coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds

        assertDoesNotThrow { mainService.getReviewById(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { reviewRepoMock.getReviewById(reviewId) }
        coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) }
        coVerify(exactly = 1) { projectRepoMock.getProjectById(project.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
        coVerify(exactly = 1) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) }
    }

    @Test
    fun `When a non project member retrieves the review, then an unauthorized exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val review = DataBuilder.createExampleReview(id = reviewId, userId = currentUser.id)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = review.projectPaperId, projectId = project.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { reviewRepoMock.getReviewById(review.id) } returns review
        coEvery { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> { mainService.getReviewById(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { reviewRepoMock.getReviewById(reviewId) }
        coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(review.projectPaperId) }
        coVerify(exactly = 1) { projectRepoMock.getProjectById(project.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
        coVerify(exactly = 0) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(any()) }
    }
}
