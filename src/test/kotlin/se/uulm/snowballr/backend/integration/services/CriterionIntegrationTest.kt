package se.uulm.snowballr.backend.integration.services

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.CriterionOuterClass.Criterion
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.Project
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CriterionIntegrationTest : IntegrationTest() {
    private suspend fun createProjectAndCriterion(criterionName: String = "Test Criterion"): Pair<Project, Criterion> {
        val project = mainService.createProject(Project.Create.newBuilder().setName("Test Project").build())
        val criterion = mainService.createCriterion(
            Criterion.Create.newBuilder()
                .setName(criterionName)
                .setTag("TC")
                .setDescription("A test criterion")
                .setCategory(CriterionCategory.CRITERION_CATEGORY_INCLUSION)
                .setProjectId(project.id)
                .build(),
        )
        return project to criterion
    }

    @Nested
    inner class CreateCriterion {
        @Test
        fun `When a criterion is created for a project, then it appears in the project's criteria list`() = runTest {
            val (project, criterion) = createProjectAndCriterion("New Criterion")
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            val criteria = mainService.getAllCriteriaForProject(projectId)

            assertTrue(criteria.criteriaList.any { it.id == criterion.id })
        }

        @Test
        fun `When a criterion is created for a project, then it can be retrieved by ID`() = runTest {
            val (_, criterion) = createProjectAndCriterion("Fetched Criterion")
            val criterionId = parseUUID(criterion.id, EntityType.CRITERION)

            val fetched = mainService.getCriterionById(criterionId)

            assertEquals(criterion.id, fetched.id)
            assertEquals("Fetched Criterion", fetched.name)
        }

        @Test
        fun `When multiple criteria are created for a project, then all appear in the criteria list`() = runTest {
            val project =
                mainService.createProject(Project.Create.newBuilder().setName("Multi Criteria Project").build())
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            mainService.createCriterion(
                Criterion.Create.newBuilder()
                    .setName("Criterion One")
                    .setTag("C1")
                    .setDescription("First")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_INCLUSION)
                    .setProjectId(project.id)
                    .build(),
            )
            mainService.createCriterion(
                Criterion.Create.newBuilder()
                    .setName("Criterion Two")
                    .setTag("C2")
                    .setDescription("Second")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
                    .setProjectId(project.id)
                    .build(),
            )

            val criteria = mainService.getAllCriteriaForProject(projectId)
            val names = criteria.criteriaList.map { it.name }

            assertTrue(names.contains("Criterion One"))
            assertTrue(names.contains("Criterion Two"))
        }
    }

    @Nested
    inner class UpdateCriterion {
        @Test
        fun `When a criterion's name is updated, then the updated name is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion("Old Name")
            val criterionId = parseUUID(criterion.id, EntityType.CRITERION)

            val updatedCriterion = criterion.toBuilder().setName("New Name").build()
            val request = Criterion.Update.newBuilder()
                .setCriterion(updatedCriterion)
                .setMask(FieldMaskUtil.fromStringList(listOf("criterion.name")))
                .build()

            val result = mainService.updateCriterion(request)

            assertEquals("New Name", result.name)

            val fetched = mainService.getCriterionById(criterionId)
            assertEquals("New Name", fetched.name)
        }

        @Test
        fun `When a criterion's category is updated, then the updated category is persisted`() = runTest {
            val (_, criterion) = createProjectAndCriterion()
            val criterionId = parseUUID(criterion.id, EntityType.CRITERION)

            val updatedCriterion = criterion.toBuilder()
                .setCategory(CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION)
                .build()
            val request = Criterion.Update.newBuilder()
                .setCriterion(updatedCriterion)
                .setMask(FieldMaskUtil.fromStringList(listOf("criterion.category")))
                .build()

            mainService.updateCriterion(request)

            val fetched = mainService.getCriterionById(criterionId)
            assertEquals(CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION, fetched.category)
        }
    }
}
