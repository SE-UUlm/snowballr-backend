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
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import se.uulm.snowballr.backend.model.outgoing.projectpaper.ProjectPaperResponse
import java.util.UUID

class CriterionIntegrationTest : IntegrationTest() {
    private suspend fun createProjectCriterion(projectId: UUID, name: String = "Test Criterion") =
        criterionService.createCriterion(
            CreateCriterionRequest(
                tag = "TC",
                name = name,
                description = "A test criterion",
                category = CriterionCategory.INCLUSION,
                projectId = projectId,
            ),
        ) as Criterion.ProjectCriterion

    private suspend fun createProjectAndCriterion(
        criterionName: String = "Test Criterion",
    ): Pair<ProjectResponse, Criterion.ProjectCriterion> {
        val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
        val criterion = createProjectCriterion(project.id, criterionName)
        return project to criterion
    }

    private suspend fun lockProject(project: ProjectResponse, projectPaper: ProjectPaperResponse): ProjectResponse {
        reviewService.createReview(
            CreateReviewRequest(
                projectPaperId = projectPaper.id,
                decision = ReviewDecision.ACCEPTED,
                selectedCriteriaIds = emptyList(),
            ),
        )

        val updatedProject = projectService.getProjectById(project.id)
        assertEquals(ProjectStatus.ACTIVE_LOCKED, updatedProject.status)

        return updatedProject
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

        @Test
        fun `When a criterion is created in a locked project, then the creation fails`() = runTest {
            val (project, projectPaper) = setupProjectAndPaperForReview()
            val lockedProject = lockProject(project, projectPaper)

            assertThrows<FailedPreconditionException> {
                createProjectCriterion(lockedProject.id)
            }
        }

        @Test
        fun `When a criterion is created for a non-existing project, then the creation fails`() = runTest {
            assertThrows<ProjectNotFoundException> {
                criterionService.createCriterion(
                    CreateCriterionRequest(
                        tag = "TC",
                        name = "Criterion name",
                        description = "A test criterion",
                        category = CriterionCategory.INCLUSION,
                        projectId = UUID.randomUUID(),
                    ),
                )
            }
        }
    }

    @Nested
    inner class UpdateCriterion {
        @Test
        fun `When a criterion's name is updated, then the updated name is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion("Old Name")
            val request = UpdateCriterionRequest.fromCriterion(criterion).copy(name = "New Name")

            val result = criterionService.updateCriterion(request, setOf(CriterionField.NAME))
            assertEquals("New Name", result.name)

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals("New Name", fetched.name)
        }

        @Test
        fun `When a criterion's category is updated, then the updated category is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion()
            val request = UpdateCriterionRequest.fromCriterion(criterion)
                .copy(category = CriterionCategory.HARD_EXCLUSION)

            criterionService.updateCriterion(request, setOf(CriterionField.CATEGORY))

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals(CriterionCategory.HARD_EXCLUSION, fetched.category)
        }

        @Test
        fun `When a criterion is updated in a locked project, then the update fails and the original state is kept`() =
            runTest {
                val (project, projectPaper) = setupProjectAndPaperForReview()
                val criterion = createProjectCriterion(project.id)
                lockProject(project, projectPaper)

                val request = UpdateCriterionRequest.fromCriterion(criterion)
                    .copy(category = CriterionCategory.HARD_EXCLUSION)

                assertThrows<FailedPreconditionException> {
                    criterionService.updateCriterion(request, setOf(CriterionField.CATEGORY))
                }

                val fetched = criterionService.getCriterionById(criterion.id)
                assertEquals(criterion.category, fetched.category)
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
        fun `When a criterion is deleted in a locked project, then the deletion fails and the criterion is kept`() =
            runTest {
                val (project, projectPaper) = setupProjectAndPaperForReview()
                val criterion = createProjectCriterion(project.id)
                lockProject(project, projectPaper)

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
