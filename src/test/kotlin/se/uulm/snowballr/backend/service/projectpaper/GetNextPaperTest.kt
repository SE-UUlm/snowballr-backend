package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.PaperNavigationDirection
import se.uulm.snowballr.backend.model.exception.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetNextPaperTest : MainServiceTest() {
    private val projectPaperId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id.newBuilder().setId(projectPaperId.toString()).build()

    @Test
    fun `When a server admin requests the next project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))
        val projectPaper =
            DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id, paperId = paper.id)
        val nextProjectPaper = DataBuilder.createExampleProjectPaper()

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(projectPaper.projectId) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(
                project.id, projectPaper.localPaperId,
                PaperNavigationDirection.NEXT,
            )
        } returns Result.success(nextProjectPaper)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id) } returns emptyList()

        assertDoesNotThrow { mainService.getNextPaper(getExampleRequest()) }
    }

    @Test
    fun `When a project member requests the next project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))
        val projectPaper =
            DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id, paperId = paper.id)
        val nextProjectPaper = DataBuilder.createExampleProjectPaper()
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(projectPaper.projectId) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(
                project.id, projectPaper.localPaperId,
                PaperNavigationDirection.NEXT,
            )
        } returns Result.success(nextProjectPaper)
        coEvery { paperRepoMock.getPaperById(nextProjectPaper.paperId) } returns Result.success(paper)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(nextProjectPaper.id) } returns emptyList()

        assertDoesNotThrow { mainService.getNextPaper(getExampleRequest()) }
    }

    @Test
    fun `When a non project member requests the next project paper, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val projectPaper = DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaper.id)
            } returns Result.success(projectPaper)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.getNextPaper(getExampleRequest()) }
        }

    @Test
    fun `When no next paper exists, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper =
            DataBuilder.createExampleProjectPaper(id = projectPaperId, projectId = project.id, paperId = paper.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectPaperRepoMock.getProjectPaperById(projectPaper.id)
        } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(projectPaper.projectId) } returns Result.success(project)
        coEvery {
            projectPaperRepoMock.getAdjacentPaper(
                project.id, projectPaper.localPaperId,
                PaperNavigationDirection.NEXT,
            )
        } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getNextPaper(getExampleRequest()) }
    }
}
