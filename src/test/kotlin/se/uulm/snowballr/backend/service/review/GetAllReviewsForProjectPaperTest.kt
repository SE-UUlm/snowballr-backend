package se.uulm.snowballr.backend.service.review

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID

class GetAllReviewsForProjectPaperTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestId.toString()).build()

    @Test
    fun `When retrieving current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getReviewById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When retrieving project paper fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(requestId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When retrieving project fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When retrieving project members fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When retrieving all reviews for project paper fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When retrieving selected criteria ids fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val review = DataBuilder.createExampleReview(userId = currentUser.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When a server admin retrieves all reviews for a project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)
        val review = DataBuilder.createExampleReview(userId = currentUser.id)
        val selectedCriteriaIds = listOf<UUID>(UUID.randomUUID())

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds

        assertDoesNotThrow { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When a project member retrieves all reviews for a project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val review = DataBuilder.createExampleReview(userId = currentUser.id)
        val selectedCriteriaIds = listOf<UUID>(UUID.randomUUID())

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns selectedCriteriaIds

        assertDoesNotThrow { mainService.getAllReviewsForProjectPaper(getExampleRequest()) }
    }

    @Test
    fun `When a non project member retrieves all reviews for a project paper, then an unauthorized exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val projectPaper = DataBuilder.createExampleProjectPaper(id = requestId, projectId = project.id)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

            assertThrows<SnowballRException.UnauthorizedException> {
                mainService.getAllReviewsForProjectPaper(
                    getExampleRequest(),
                )
            }
        }
}
