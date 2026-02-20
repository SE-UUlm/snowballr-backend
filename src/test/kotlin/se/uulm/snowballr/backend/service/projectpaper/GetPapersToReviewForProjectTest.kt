package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPapersToReviewForProjectTest : MainServiceTest() {
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
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns !isUserAdmin
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @Test
    fun `When a server admin requests the project papers to review, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockHappyPath(true, projectId)

        assertDoesNotThrow { mainService.getPapersToReviewForProject(projectId) }
    }

    @Test
    fun `When a project member requests the project papers to review, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockHappyPath(false, projectId)

        assertDoesNotThrow { mainService.getPapersToReviewForProject(projectId) }
    }

    @Test
    fun `When a non project member requests the project papers to review, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns false

            assertThrows<UnauthorizedException> {
                mainService.getPapersToReviewForProject(project.id)
            }
        }

    @Test
    fun `When the project papers to review are requested, then only the undecided papers are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))
        val projectPaperAlreadyDecided = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.PAPER_DECISION_ACCEPTED,
        )
        val projectPaperNotAlreadyDecided = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
        )
        val projectPaperWithPaper1 = ProjectPaperWithPaper(projectPaperAlreadyDecided, paper)
        val projectPaperWithPaper2 = ProjectPaperWithPaper(projectPaperNotAlreadyDecided, paper)
        val review = DataBuilder.createExampleReview(userId = UUID.randomUUID())

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns false
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper1, projectPaperWithPaper2)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(projectPaperAlreadyDecided.id)
        } returns listOf(review)
        coEvery {
            reviewRepoMock.getAllReviewsForProjectPaper(projectPaperNotAlreadyDecided.id)
        } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())

        var projectPapers: GrpcProjectPaper.List
        assertDoesNotThrow { projectPapers = mainService.getPapersToReviewForProject(project.id) }
        assertThat(projectPapers.projectPapersList).hasSize(1)
        assertThat(projectPapers.projectPapersList).anyMatch { it.id == projectPaperNotAlreadyDecided.id.toString() }
        assertThat(projectPapers.projectPapersList).noneMatch { it.id == projectPaperAlreadyDecided.id.toString() }
    }

    @Test
    fun `When the project papers to review are requested, then only undecided papers that were not already reviewed by the current user are returned`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val project = DataBuilder.createExampleProject()
            val author = DataBuilder.createExampleAuthor()
            val paper = DataBuilder.createExamplePaper(authors = listOf(author))
            val projectPaperWithCurrentUserReview = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper.id,
                decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
            )
            val projectPaperWithoutCurrentUserReview = DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                paperId = paper.id,
                decision = PaperDecision.PAPER_DECISION_UNREVIEWED,
            )
            val projectPaperWithPaper1 = ProjectPaperWithPaper(projectPaperWithCurrentUserReview, paper)
            val projectPaperWithPaper2 = ProjectPaperWithPaper(projectPaperWithoutCurrentUserReview, paper)
            val reviewByCurrentUser = DataBuilder.createExampleReview(userId = currentUser.id)
            val reviewByOtherUser = DataBuilder.createExampleReview(userId = UUID.randomUUID())

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns false
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery {
                projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
            } returns listOf(projectPaperWithPaper1, projectPaperWithPaper2)
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } returns listOf(UUID.randomUUID())
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaperWithCurrentUserReview.id)
            } returns listOf(reviewByCurrentUser)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaperWithoutCurrentUserReview.id)
            } returns listOf(reviewByOtherUser)
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(reviewByCurrentUser.id)
            } returns listOf(UUID.randomUUID())
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(reviewByOtherUser.id)
            } returns listOf(UUID.randomUUID())

            var projectPapers: GrpcProjectPaper.List
            assertDoesNotThrow { projectPapers = mainService.getPapersToReviewForProject(project.id) }
            assertThat(projectPapers.projectPapersList).hasSize(1)
            assertThat(projectPapers.projectPapersList)
                .anyMatch { it.id == projectPaperWithoutCurrentUserReview.id.toString() }
            assertThat(projectPapers.projectPapersList)
                .noneMatch { it.id == projectPaperWithCurrentUserReview.id.toString() }
        }

    @Test
    fun `When a nonexistent project is requested, then a ProjectNotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<ProjectNotFoundException> {
            mainService.getPapersToReviewForProject(project.id)
        }
    }
}
