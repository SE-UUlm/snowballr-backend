package se.uulm.snowballr.backend.integration.services

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.criterion.toGrpcCriterion
import se.uulm.snowballr.backend.model.incoming.CreateCriterionRequest
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass.Project
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CriterionIntegrationTest : IntegrationTest() {
    private suspend fun createProjectAndCriterion(
        criterionName: String = "Test Criterion",
    ): Pair<Project, Criterion.ProjectCriterion> {
        val project = projectService.createProject(Project.Create.newBuilder().setName("Test Project").build())
        val criterion = criterionService.createCriterion(
            CreateCriterionRequest(
                tag = "TC",
                name = criterionName,
                description = "A test criterion",
                category = CriterionCategory.INCLUSION,
                projectId = parseUUID(project.id, EntityType.PROJECT),
            ),
        ) as Criterion.ProjectCriterion
        return project to criterion
    }

    @Nested
    inner class CreateCriterion {
        @Test
        fun `When a criterion is created for a project, then it appears in the project's criteria list`() = runTest {
            val (project, criterion) = createProjectAndCriterion("New Criterion")
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            val criteria = criterionService.getAllCriteriaForProject(projectId)

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
            val project =
                projectService.createProject(Project.Create.newBuilder().setName("Multi Criteria Project").build())
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            criterionService.createCriterion(
                CreateCriterionRequest(
                    tag = "C1",
                    name = "Criterion One",
                    description = "First",
                    category = CriterionCategory.INCLUSION,
                    projectId = projectId,
                ),
            )
            criterionService.createCriterion(
                CreateCriterionRequest(
                    tag = "C2",
                    name = "Criterion Two",
                    description = "Second",
                    category = CriterionCategory.EXCLUSION,
                    projectId = projectId,
                ),
            )

            val criteria = criterionService.getAllCriteriaForProject(projectId)
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

            val updatedCriterion = criterion.copy(name = "New Name")
            val request = CriterionOuterClass.Criterion.Update.newBuilder()
                .setCriterion(updatedCriterion.toGrpcCriterion())
                .setMask(FieldMaskUtil.fromStringList(listOf("criterion.name")))
                .build()

            val result = criterionService.updateCriterion(request)

            assertEquals("New Name", result.name)

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals("New Name", fetched.name)
        }

        @Test
        fun `When a criterion's category is updated, then the updated category is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion()

            val updatedCriterion = criterion.copy(category = CriterionCategory.HARD_EXCLUSION)
            val request = CriterionOuterClass.Criterion.Update.newBuilder()
                .setCriterion(updatedCriterion.toGrpcCriterion())
                .setMask(FieldMaskUtil.fromStringList(listOf("criterion.category")))
                .build()

            criterionService.updateCriterion(request)

            val fetched = criterionService.getCriterionById(criterion.id)
            assertEquals(CriterionCategory.HARD_EXCLUSION, fetched.category)
        }
    }
}
