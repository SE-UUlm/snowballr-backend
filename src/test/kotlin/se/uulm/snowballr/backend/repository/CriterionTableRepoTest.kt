package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import snowballr.CriterionOuterClass
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class CriterionTableRepoTest : H2DatabaseTest(arrayOf(CriterionTable, ProjectTable), true) {
    private val repo = CriterionTableRepo(db)
    private val projectRepo = ProjectTableRepo(db)

    private suspend fun createExampleProject(): Project {
        val request =
            ProjectOuterClass.Project.Create
                .newBuilder()
                .build()
        return projectRepo.createProject(request, testUserId)
    }

    private suspend fun insertTestCriterionAndGetId(
        tag: String = "Test Tag",
        name: String = "Test Criterion",
        description: String = "Test Description",
        category: CriterionCategory = CriterionCategory.CRITERION_CATEGORY_EXCLUSION,
        projectId: UUID = UUID.randomUUID(),
    ): UUID = db.dbQuery {
        CriterionTable.insertAndGetId {
            it[CriterionTable.tag] = tag
            it[CriterionTable.name] = name
            it[CriterionTable.description] = description
            it[CriterionTable.category] = category
            it[CriterionTable.projectId] = projectId
            it[CriterionTable.createdBy] = testUserId
        }.value
    }

    companion object {
        @JvmStatic
        fun validFieldMasks(): List<Arguments> = listOf(
            Arguments.of(listOf("criterion.tag")),
            Arguments.of(listOf("criterion.name")),
            Arguments.of(listOf("criterion.description")),
            Arguments.of(listOf("criterion.category")),
        )
    }

    @Nested
    inner class GetCriterionById {
        @Test
        fun `When a criterion is found, then the correct criterion is returned`() = runTest {
            val projectId = createExampleProject().id
            val criterionId = insertTestCriterionAndGetId(projectId = projectId)
            val criterion = repo.getCriterionById(criterionId)

            assertThat(criterion.id).isEqualTo(criterionId)
            assertThat(criterion.tag).isEqualTo("Test Tag")
            assertThat(criterion.name).isEqualTo("Test Criterion")
            assertThat(criterion.description).isEqualTo("Test Description")
            assertThat(
                criterion.category,
            ).isEqualTo(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            assertThat(criterion.projectId).isEqualTo(projectId)
        }

        @Test
        fun `When a criterion is not found, then an exception is thrown`() = runTest {
            assertThrows<NotFoundException> { repo.getCriterionById(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class CreateCriterion {
        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a criterion is created, then the values are correctly assigned`(category: CriterionCategory) =
            runTest {
                val project = createExampleProject()

                val request =
                    CriterionOuterClass.Criterion.Create
                        .newBuilder()
                        .setTag("Test Tag")
                        .setName("Test Criterion")
                        .setDescription("Test Description")
                        .setCategory(category)
                        .setProjectId(project.id.toString())
                        .build()
                val criterion = repo.createCriterion(request, testUserId)

                Assertions.assertThat(criterion.tag).isEqualTo("Test Tag")
                Assertions.assertThat(criterion.name).isEqualTo("Test Criterion")
                Assertions.assertThat(criterion.description).isEqualTo("Test Description")
                Assertions.assertThat(criterion.category).isEqualTo(category)
            }

        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a criterion is created, but the assigned project doesn't exist, then an exception is thrown`(
            category: CriterionOuterClass.CriterionCategory,
        ) = runTest {
            val request =
                CriterionOuterClass.Criterion.Create
                    .newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(category)
                    .setProjectId(UUID.randomUUID().toString())
                    .build()

            assertThrows<NotFoundException> { repo.createCriterion(request, testUserId) }
        }

        @Test
        fun `When two criteria are created, then they have different IDs`() = runTest {
            val project = createExampleProject()

            val request =
                CriterionOuterClass.Criterion.Create
                    .newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
                    .setProjectId(project.id.toString())
                    .build()
            val criterion1 = repo.createCriterion(request, testUserId)
            val criterion2 = repo.createCriterion(request, testUserId)

            Assertions.assertThat(criterion1.id).isNotEqualTo(criterion2.id)
        }

        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a criterion is created, but the assigned user doesn't exist, then an exception is thrown`(
            category: CriterionCategory,
        ) = runTest {
            val request =
                CriterionOuterClass.Criterion.Create
                    .newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(category)
                    .setProjectId(UUID.randomUUID().toString())
                    .build()

            assertThrows<NotFoundException> { repo.createCriterion(request, UUID.randomUUID()) }
        }
    }

    @Nested
    inner class UpdateCriterion {
        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.CriterionTableRepoTest#validFieldMasks")
        fun `When a criterion is updated, then only the fields specified in the field mask are updated and the updated criterion is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val projectId = createExampleProject().id
            val criterionId = insertTestCriterionAndGetId(projectId = projectId)
            val originalCriterion = repo.getCriterionById(criterionId)

            val updatedCriterionDetails = originalCriterion.toGrpcCriterion().toBuilder()
                .setTag("Updated Tag")
                .setName("Updated Criterion")
                .setDescription("Updated Description")
                .setCategory(CriterionCategory.CRITERION_CATEGORY_INCLUSION)
                .build()

            val request = CriterionOuterClass.Criterion.Update.newBuilder()
                .setCriterion(updatedCriterionDetails)
                .setMask(FieldMaskUtil.fromStringList(fieldMask))
                .build()

            val updatedCriterion = repo.updateCriterion(request)

            if ("criterion.tag" in fieldMask) {
                assertThat(updatedCriterion.tag).isEqualTo("Updated Tag")
            } else {
                assertThat(updatedCriterion.tag).isEqualTo("Test Tag")
            }
            if ("criterion.name" in fieldMask) {
                assertThat(updatedCriterion.name).isEqualTo("Updated Criterion")
            } else {
                assertThat(updatedCriterion.name).isEqualTo("Test Criterion")
            }
            if ("criterion.description" in fieldMask) {
                assertThat(updatedCriterion.description).isEqualTo("Updated Description")
            } else {
                assertThat(updatedCriterion.description).isEqualTo("Test Description")
            }
            if ("criterion.category" in fieldMask) {
                assertThat(updatedCriterion.category).isEqualTo(CriterionCategory.CRITERION_CATEGORY_INCLUSION)
            } else {
                assertThat(updatedCriterion.category).isEqualTo(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            }
        }
    }
}
