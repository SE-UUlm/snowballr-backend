package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.criterion.CriterionField
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import java.util.UUID

class CriterionIntegrationTest : IntegrationTest() {
    private suspend fun createProjectAndCriterion(
        criterionName: String = "Test Criterion",
    ): Pair<ProjectResponse, Criterion.ProjectCriterion> {
        val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
        val criterion = criterionService.createCriterion(
            CreateCriterionRequest(
                tag = "TC",
                name = criterionName,
                description = "A test criterion",
                category = CriterionCategory.INCLUSION,
                projectId = project.id,
            ),
        ) as Criterion.ProjectCriterion
        return project to criterion
    }

    @Nested
    inner class CreateCriterion {
        @Test
        fun `When a criterion is created for a project, then it appears in the project's criteria list`() = runTest {
            val (project, criterion) = createProjectAndCriterion("New Criterion")

            val criteria = criterionService.getAllCriteriaForProject(project.id)

            assertTrue(criteria.any { it.id == criterion.id })
        }

        @Test
        fun `When a criterion is created for a project, then it can be retrieved by ID`() = runTest {
            val (_, criterion) = createProjectAndCriterion("Fetched Criterion")

            val fetched = criterionService.getCriterionById(criterion.id)

            assertEquals(criterion.id, fetched.id)
            assertEquals("Fetched Criterion", fetched.name)
        }

        @Test
        fun `When multiple criteria are created for a project, then all appear in the criteria list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Multi Criteria Project"))

            criterionService.createCriterion(
                CreateCriterionRequest(
                    tag = "C1",
                    name = "Criterion One",
                    description = "First",
                    category = CriterionCategory.INCLUSION,
                    projectId = project.id,
                ),
            )
            criterionService.createCriterion(
                CreateCriterionRequest(
                    tag = "C2",
                    name = "Criterion Two",
                    description = "Second",
                    category = CriterionCategory.EXCLUSION,
                    projectId = project.id,
                ),
            )

            val criteria = criterionService.getAllCriteriaForProject(project.id)
            val names = criteria.map { it.name }

            assertTrue(names.contains("Criterion One"))
            assertTrue(names.contains("Criterion Two"))
        }
    }

    @Nested
    inner class UpdateCriterion {
        @Test
        fun `When a criterion's name is updated, then the updated name is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion("Old Name")
            val request = UpdateCriterionRequest(
                criterionId = criterion.id,
                tag = criterion.tag,
                name = "New Name",
                description = criterion.description,
                category = criterion.category,
            )

            val result = criterionService.updateCriterion(request, setOf(CriterionField.NAME))
            assertEquals("New Name", result.name)

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals("New Name", fetched.name)
        }

        @Test
        fun `When a criterion's category is updated, then the updated category is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion()
            val request = UpdateCriterionRequest(
                criterionId = criterion.id,
                tag = criterion.tag,
                name = criterion.name,
                description = criterion.description,
                category = CriterionCategory.HARD_EXCLUSION,
            )

            criterionService.updateCriterion(request, setOf(CriterionField.CATEGORY))

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals(CriterionCategory.HARD_EXCLUSION, fetched.category)
        }
    }

    @Nested
    inner class DeleteCriterion {
        @Test
        fun `When a criterion is deleted, then it no longer appears in the project's criteria list`() = runTest {
            val (project, criterion) = createProjectAndCriterion()

            criterionService.deleteCriterion(criterion.id)

            val criteria = criterionService.getAllCriteriaForProject(project.id)
            assertFalse(criteria.any { it.id == criterion.id })
            assertThrows<NotFoundException> { criterionService.getCriterionById(criterion.id) }
        }

        @Test
        fun `When a criterion is associated with a review, then deleting it fails and it is kept`() = runTest {
            val (project, criterion) = createProjectAndCriterion()
            val projectPaper = addToProject(project, createPaper())
            reviewService.createReview(
                CreateReviewRequest(
                    projectPaperId = projectPaper.id,
                    decision = ReviewDecision.DECLINED,
                    selectedCriteriaIds = listOf(criterion.id),
                ),
            )

            assertThrows<FailedPreconditionException> { criterionService.deleteCriterion(criterion.id) }

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals(criterion.id, fetched.id)
        }

        @Test
        fun `When a non-existing criterion is deleted, then a NotFoundException is thrown`() = runTest {
            assertThrows<NotFoundException> { criterionService.deleteCriterion(UUID.randomUUID()) }
        }
    }
}
