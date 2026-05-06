package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateProjectPaperException
import se.uulm.snowballr.backend.model.exception.invalidargument.StageOutOfRangeException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID
import kotlin.test.assertEquals
import snowballr.ProjectOuterClass.Project as GrpcProject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddPaperToProjectTest : MainServiceTest() {
    private fun getRequest(projectId: UUID, paperId: UUID, stage: Long = 0) = GrpcProject.Paper.Add.newBuilder()
        .setProjectId(projectId.toString())
        .setPaperId(paperId.toString())
        .setStage(stage)
        .build()

    @Test
    fun `When the user adds a paper to a project and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.success(project)
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)

        val backwardReferences = listOf(UUID.randomUUID(), UUID.randomUUID())
        val reviews = listOf(DataBuilder.createExampleReview(), DataBuilder.createExampleReview())
        val criteriaIds0 = listOf(UUID.randomUUID(), UUID.randomUUID())
        val criteriaIds1 = listOf(UUID.randomUUID(), UUID.randomUUID())

        val request = getRequest(project.id, paper.id)

        mockCurrentUser(user)
        coJustRun { projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false
        coEvery { projectPaperRepoMock.addPaperToProject(request, user.id) } returns projectPaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns backwardReferences
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns reviews
        coEvery { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(reviews[0].id) } returns criteriaIds0
        coEvery { reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(reviews[1].id) } returns criteriaIds1

        val addedProjectPaper = mainService.addPaperToProject(request)

        assertEquals(2, addedProjectPaper.reviewsCount)
        val review0 = addedProjectPaper.getReviews(0)
        val review1 = addedProjectPaper.getReviews(1)
        assertEquals(review0.id, reviews[0].id.toString())
        assertEquals(review1.id, reviews[1].id.toString())

        val criteriaIdsReview0 = review0.selectedCriteriaIdsList
        val criteriaIdsReview1 = review1.selectedCriteriaIdsList
        assertEquals(2, criteriaIdsReview0.size)
        assertEquals(2, criteriaIdsReview1.size)
        assertEquals(criteriaIds0[0].toString(), criteriaIdsReview0[0].toString())
        assertEquals(criteriaIds0[1].toString(), criteriaIdsReview0[1].toString())
        assertEquals(criteriaIds1[0].toString(), criteriaIdsReview1[0].toString())
        assertEquals(criteriaIds1[1].toString(), criteriaIdsReview1[1].toString())
    }

    @Test
    fun `When the user adds a paper to a project, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val projectResult = Result.success(project)
            val paper = DataBuilder.createExamplePaper()

            val request = getRequest(project.id, paper.id)

            mockCurrentUser(user)
            coEvery {
                projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult)
            } throws TestSpecificException()
            coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult

            assertThrows<TestSpecificException> { mainService.addPaperToProject(request) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.failure<Project>(TestSpecificException())
        val paper = DataBuilder.createExamplePaper()

        val request = getRequest(project.id, paper.id)

        mockCurrentUser(user)
        coJustRun { projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult

        assertThrows<TestSpecificException> { mainService.addPaperToProject(request) }
    }

    @Test
    fun `When retrieving the paper fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.success(project)
        val paper = DataBuilder.createExamplePaper()

        val request = getRequest(project.id, paper.id)

        mockCurrentUser(user)
        coJustRun { projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { paperRepoMock.getPaperById(paper.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.addPaperToProject(request) }
    }

    @Test
    fun `When the project paper already exists, then a DuplicateProjectPaperException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.success(project)
        val paper = DataBuilder.createExamplePaper()

        val request = getRequest(project.id, paper.id)

        mockCurrentUser(user)
        coJustRun { projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns true

        assertThrows<DuplicateProjectPaperException> { mainService.addPaperToProject(request) }
    }

    @Test
    fun `When the requested stage is greater than the projects max stage, then a StageOutOfRangeException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val projectResult = Result.success(project)
            val paper = DataBuilder.createExamplePaper()

            val request = getRequest(project.id, paper.id, stage = 1)

            mockCurrentUser(user)
            coJustRun { projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
            coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
            coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false

            assertThrows<StageOutOfRangeException> { mainService.addPaperToProject(request) }
        }

    @Test
    fun `When the requested stage is negative, then a StageOutOfRangeException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.success(project)
        val paper = DataBuilder.createExamplePaper()

        val request = getRequest(project.id, paper.id, stage = -1)

        mockCurrentUser(user)
        coJustRun { projectPaperAccessCheckerMock.isAllowedToAddPaperToProject(user, project.id, projectResult) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false

        assertThrows<StageOutOfRangeException> { mainService.addPaperToProject(request) }
    }
}
