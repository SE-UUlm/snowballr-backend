package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllProjectPapersForProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id.newBuilder().setId(projectId.toString()).build()

    @Suppress("LongMethod")
    private fun mockHappyPath(isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val paper = DataBuilder.createExamplePaper(id = projectId)
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
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
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
        mockHappyPath(true)

        assertDoesNotThrow { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a project member requests the project papers, then no exception is thrown`() = runTest {
        mockHappyPath(false)

        assertDoesNotThrow { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a non project member requests the project papers, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> {
            mainService.getAllProjectPapersForProject(getExampleRequest())
        }
    }

    @Test
    fun `When a nonexistent project is requested, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns false

        assertThrows<NotFoundException> {
            mainService.getAllProjectPapersForProject(getExampleRequest())
        }
    }
}
