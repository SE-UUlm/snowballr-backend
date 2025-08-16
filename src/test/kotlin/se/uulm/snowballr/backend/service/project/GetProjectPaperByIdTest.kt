package se.uulm.snowballr.backend.service.project

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
class GetProjectPaperByIdTest : MainServiceTest() {
    private val projectPaperId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(projectPaperId.toString()).build()

    // Test data defined at class level for access in mock setup and verification
    private val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
    private val project = DataBuilder.createExampleProject()
    private val paper = DataBuilder.createExamplePaper()
    private val projectPaper = DataBuilder.createExampleProjectPaper(
        id = projectPaperId,
        projectId = project.id,
        paperId = paper.id,
    )
    private val author = DataBuilder.createExampleAuthor()
    private val review = DataBuilder.createExampleReview(projectPaperId = projectPaper.id)

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(GrpcContext::getUserIdFromContext),
        Arguments.of(userRepoMock::getUserById),
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(projectMemberRepoMock::getProjectMembers),
        Arguments.of(paperRepoMock::getPaperById),
        Arguments.of(authorOfPaperRepoMock::getAuthorsOfPaperById),
        Arguments.of(citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById),
        Arguments.of(reviewRepoMock::getAllReviewsForProjectPaper),
        Arguments.of(reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById),
    )

    @Suppress("LongMethod", "ReturnCount")
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

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } throws TestSpecificException()
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper

        if (failAt == projectMemberRepoMock::getProjectMembers) {
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } throws TestSpecificException()
            return
        }
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper

        if (failAt == authorOfPaperRepoMock::getAuthorsOfPaperById) {
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } throws TestSpecificException()
            return
        }
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)

        if (failAt == citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        if (failAt == reviewRepoMock::getAllReviewsForProjectPaper) {
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id)
            } throws TestSpecificException()
            return
        }
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)

        if (failAt == reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById) {
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then an exception is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt)
        assertThrows<TestSpecificException> {
            mainService.getProjectPaperById(getExampleRequest())
        }

        when (failAt) {
            GrpcContext::getUserIdFromContext -> {
                verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
                coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
            }

            userRepoMock::getUserById -> {
                verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
                coVerify(exactly = 1) { userRepoMock.getUserById(adminUser.id) }
                coVerify(exactly = 0) { projectPaperRepoMock.getProjectPaperById(any()) }
            }

            projectPaperRepoMock::getProjectPaperById -> {
                coVerify(exactly = 1) { userRepoMock.getUserById(adminUser.id) }
                coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(projectPaperId) }
                coVerify(exactly = 0) { projectMemberRepoMock.getProjectMembers(any()) }
            }

            projectMemberRepoMock::getProjectMembers -> {
                coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(projectPaperId) }
                coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
                coVerify(exactly = 0) { paperRepoMock.getPaperById(any()) }
            }

            paperRepoMock::getPaperById -> {
                coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
                coVerify(exactly = 1) { paperRepoMock.getPaperById(paper.id) }
                coVerify(exactly = 0) { authorOfPaperRepoMock.getAuthorsOfPaperById(any()) }
            }

            authorOfPaperRepoMock::getAuthorsOfPaperById -> {
                coVerify(exactly = 1) { paperRepoMock.getPaperById(paper.id) }
                coVerify(exactly = 1) { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) }
                coVerify(exactly = 0) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(any()) }
            }

            citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById -> {
                coVerify(exactly = 1) { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) }
                coVerify(exactly = 1) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) }
                coVerify(exactly = 0) { reviewRepoMock.getAllReviewsForProjectPaper(any()) }
            }

            reviewRepoMock::getAllReviewsForProjectPaper -> {
                coVerify(exactly = 1) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) }
                coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) }
                coVerify(exactly = 0) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(any()) }
            }

            reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById -> {
                coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) }
                coVerify(exactly = 1) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) }
            }
        }
    }

    @Test
    fun `When a server admin retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null)
        assertDoesNotThrow { mainService.getProjectPaperById(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(adminUser.id) }
        coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(projectPaperId) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
        coVerify(exactly = 1) { paperRepoMock.getPaperById(paper.id) }
        coVerify(exactly = 1) { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) }
        coVerify(exactly = 1) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) }
        coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) }
        coVerify(exactly = 1) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) }
    }

    @Test
    fun `When a project member retrieves the project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            paperId = paper.id,
        )
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview(projectPaperId = projectPaper.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getProjectPaperById(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(projectPaperId) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
        coVerify(exactly = 1) { paperRepoMock.getPaperById(paper.id) }
        coVerify(exactly = 1) { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) }
        coVerify(exactly = 1) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) }
        coVerify(exactly = 1) { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) }
        coVerify(exactly = 1) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id) }
    }

    @Test
    fun `When a non project member retrieves the project paper, then an unauthorized exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            paperId = paper.id,
        )

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> { mainService.getProjectPaperById(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { projectPaperRepoMock.getProjectPaperById(projectPaperId) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(project.id) }
        coVerify(exactly = 0) { paperRepoMock.getPaperById(any()) }
        coVerify(exactly = 0) { authorOfPaperRepoMock.getAuthorsOfPaperById(any()) }
        coVerify(exactly = 0) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(any()) }
        coVerify(exactly = 0) { reviewRepoMock.getAllReviewsForProjectPaper(any()) }
        coVerify(exactly = 0) { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(any()) }
    }
}
