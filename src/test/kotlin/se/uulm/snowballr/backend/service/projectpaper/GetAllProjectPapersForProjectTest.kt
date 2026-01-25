package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllProjectPapersForProjectTest : MainServiceTest() {
    @Suppress("LongMethod")
    private fun mockHappyPath(isUserAdmin: Boolean, projectId: UUID) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(id = projectId, authors = listOf(author))
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersWithPapers(project.id) } returns listOf(projectPaperWithPaper)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @Test
    fun `When a server admin requests the project papers, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockHappyPath(true, projectId)

        assertDoesNotThrow { mainService.getAllProjectPapersForProject(projectId) }
    }

    @Test
    fun `When a project member requests the project papers, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockHappyPath(false, projectId)

        assertDoesNotThrow { mainService.getAllProjectPapersForProject(projectId) }
    }

    @Test
    fun `When a non project member requests the project papers, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> {
            mainService.getAllProjectPapersForProject(project.id)
        }
    }

    @Test
    fun `When a nonexistent project is requested, then a ProjectNotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<ProjectNotFoundException> {
            mainService.getAllProjectPapersForProject(project.id)
        }
    }
}
