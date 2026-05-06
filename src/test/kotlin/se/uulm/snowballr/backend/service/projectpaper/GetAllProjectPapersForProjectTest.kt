package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllProjectPapersForProjectTest : MainServiceTest() {
    @Test
    @Suppress("LongMethod")
    fun `When the user requests all project papers for a project and has access, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val projectPaper0 = DataBuilder.createExampleProjectPaperWithPaper()
            val projectPaper0Refs = listOf(UUID.randomUUID(), UUID.randomUUID())
            val projectPaper0Review0 = DataBuilder.createExampleReview()
            val projectPaper0Review1 = DataBuilder.createExampleReview()
            val projectPaper1 = DataBuilder.createExampleProjectPaperWithPaper()
            val projectPaper1Refs = listOf(UUID.randomUUID(), UUID.randomUUID())
            val projectPaper1Review0 = DataBuilder.createExampleReview()
            val projectPaper1Review1 = DataBuilder.createExampleReview()
            val projectPapersWithPapers = listOf(projectPaper0, projectPaper1)
            val criterion0 = DataBuilder.createExampleProjectCriterion()
            val criterion1 = DataBuilder.createExampleProjectCriterion()
            val criterion2 = DataBuilder.createExampleProjectCriterion()

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { projectPaperRepoMock.getAllProjectPapersWithPapers(project.id) } returns projectPapersWithPapers
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(projectPaper0.paper.id)
            } returns projectPaper0Refs
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(projectPaper1.paper.id)
            } returns projectPaper1Refs
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaper0.projectPaper.id)
            } returns listOf(projectPaper0Review0, projectPaper0Review1)
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaper1.projectPaper.id)
            } returns listOf(projectPaper1Review0, projectPaper1Review1)
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(projectPaper0Review0.id)
            } returns listOf(criterion0.id, criterion1.id)
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(projectPaper0Review1.id)
            } returns listOf(criterion2.id, criterion1.id)
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(projectPaper1Review0.id)
            } returns listOf(criterion2.id, criterion0.id)
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(projectPaper1Review1.id)
            } returns listOf(criterion0.id, criterion2.id)

            val projectPapers = mainService.getAllProjectPapersForProject(project.id).projectPapersList

            assertEquals(2, projectPapers.size)
            assertEquals(projectPaper0.projectPaper.id.toString(), projectPapers[0].id)
            assertEquals(projectPaper1.projectPaper.id.toString(), projectPapers[1].id)
            val projectPaper0Result = projectPapers[0]
            val projectPaper1Result = projectPapers[1]
            val backwardRefs0 = projectPaper0Result.paper.backwardReferencedIdsList
            val backwardRefs1 = projectPaper1Result.paper.backwardReferencedIdsList
            assertEquals(projectPaper0Refs.size, backwardRefs0.size)
            assertEquals(projectPaper1Refs.size, backwardRefs1.size)
            projectPaper0Refs.forEach { refId -> assertContains(backwardRefs0, refId.toString()) }
            projectPaper1Refs.forEach { refId -> assertContains(backwardRefs1, refId.toString()) }
            val reviews0 = projectPaper0Result.reviewsList
            val reviews1 = projectPaper1Result.reviewsList
            assertEquals(2, reviews0.size)
            assertEquals(2, reviews1.size)
            val review0CriterionIds0 = reviews0[0].selectedCriteriaIdsList
            val review0CriterionIds1 = reviews0[1].selectedCriteriaIdsList
            val review1CriterionIds0 = reviews1[0].selectedCriteriaIdsList
            val review1CriterionIds1 = reviews1[1].selectedCriteriaIdsList
            assertEquals(2, review0CriterionIds0.size)
            assertEquals(2, review0CriterionIds1.size)
            assertEquals(2, review1CriterionIds0.size)
            assertEquals(2, review1CriterionIds1.size)
            assertContains(review0CriterionIds0, criterion0.id.toString())
            assertContains(review0CriterionIds0, criterion1.id.toString())
            assertContains(review0CriterionIds1, criterion2.id.toString())
            assertContains(review0CriterionIds1, criterion1.id.toString())
            assertContains(review1CriterionIds0, criterion2.id.toString())
            assertContains(review1CriterionIds0, criterion0.id.toString())
            assertContains(review1CriterionIds1, criterion0.id.toString())
            assertContains(review1CriterionIds1, criterion2.id.toString())
        }

    @Test
    fun `When the user requests all project papers for a project, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(user, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.getAllProjectPapersForProject(project.id) }
        }
}
