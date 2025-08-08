package se.uulm.snowballr.backend.service.project

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
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID

class GetAllProjectPapersForProjectTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestId.toString()).build()

    @Test
    fun `When retrieving current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested project fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested project members fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = requestId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving project papers with project fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving authors fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving backward references fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val author = DataBuilder.createExampleAuthor()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving reviews fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = requestId)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val author = DataBuilder.createExampleAuthor()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When retrieving selected criteria ids fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = requestId)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a server admin requests the project papers, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = requestId)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a project member requests the project papers, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a non project member requests the project papers, then an unauthorized exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = requestId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> {
            mainService.getAllProjectPapersForProject(
                getExampleRequest(),
            )
        }
    }
}
