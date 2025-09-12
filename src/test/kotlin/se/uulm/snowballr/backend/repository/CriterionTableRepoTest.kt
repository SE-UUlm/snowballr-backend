package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.Criterion.UserCriterion
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.repository.RepositoryHelper.createExampleUser
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertCriterionAndGetId
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass
import java.util.UUID
import kotlin.test.assertIs
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

class CriterionTableRepoTest : RepositoryTest(arrayOf(CriterionTable, ProjectTable), true) {
    private val repo = CriterionTableRepo(db)
    private val projectRepo = ProjectTableRepo(db)

    private suspend fun createExampleProject(): Project {
        val request =
            ProjectOuterClass.Project.Create
                .newBuilder()
                .build()
        return projectRepo.createProject(request, testUserId, DataBuilder.createExampleUserSettings())
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
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            val result = repo.getCriterionById(criterionId)

            val criterion = assertResultSuccess(result)
            assertIs<ProjectCriterion>(criterion)
            assertThat(criterion.id).isEqualTo(criterionId)
            assertThat(criterion.tag).isEqualTo("Test Tag")
            assertThat(criterion.name).isEqualTo("Test Criterion")
            assertThat(criterion.description).isEqualTo("Test Description")
            assertThat(criterion.category).isEqualTo(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            assertThat(criterion.projectId).isEqualTo(projectId)
        }

        @Test
        fun `When a criterion is not found, then an exception is thrown`() = runTest {
            val result = repo.getCriterionById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class CreateCriterion {
        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a project criterion is created, then the values are correctly assigned`(category: CriterionCategory) =
            runTest {
                val project = createExampleProject()

                val request =
                    GrpcCriterion.Create
                        .newBuilder()
                        .setTag("Test Tag")
                        .setName("Test Criterion")
                        .setDescription("Test Description")
                        .setCategory(category)
                        .setProjectId(project.id.toString())
                        .build()
                val criterion = repo.createCriterion(request, testUserId)

                assertIs<ProjectCriterion>(criterion)
                assertThat(criterion.tag).isEqualTo("Test Tag")
                assertThat(criterion.name).isEqualTo("Test Criterion")
                assertThat(criterion.description).isEqualTo("Test Description")
                assertThat(criterion.category).isEqualTo(category)
                assertThat(criterion.projectId).isEqualTo(project.id)
                assertThat(criterion.createdBy).isEqualTo(testUserId)
            }

        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a user criterion is created, then the values are correctly assigned`(category: CriterionCategory) =
            runTest {
                val request =
                    GrpcCriterion.Create
                        .newBuilder()
                        .setTag("Test Tag")
                        .setName("Test Criterion")
                        .setDescription("Test Description")
                        .setCategory(category)
                        .build()
                val criterion = repo.createCriterion(request, testUserId)

                assertIs<UserCriterion>(criterion)
                assertThat(criterion.tag).isEqualTo("Test Tag")
                assertThat(criterion.name).isEqualTo("Test Criterion")
                assertThat(criterion.description).isEqualTo("Test Description")
                assertThat(criterion.category).isEqualTo(category)
                assertThat(criterion.createdBy).isEqualTo(testUserId)
            }

        @Test
        fun `When a project criterion is created, but the assigned project doesn't exist, then an exception is thrown`() =
            runTest {
                val request =
                    GrpcCriterion.Create
                        .newBuilder()
                        .setTag("Test Tag")
                        .setName("Test Criterion")
                        .setDescription("Test Description")
                        .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
                        .setProjectId(UUID.randomUUID().toString())
                        .build()

                assertThrows<NotFoundException> { repo.createCriterion(request, testUserId) }
            }

        @Test
        fun `When two criteria are created, then they have different IDs`() = runTest {
            val project = createExampleProject()

            val request =
                GrpcCriterion.Create
                    .newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
                    .setProjectId(project.id.toString())
                    .build()
            val criterion1 = repo.createCriterion(request, testUserId)
            val criterion2 = repo.createCriterion(request, testUserId)

            assertThat(criterion1.id).isNotEqualTo(criterion2.id)
        }

        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a criterion is created, but the assigned user doesn't exist, then an exception is thrown`(
            category: CriterionCategory,
        ) = runTest {
            val request =
                GrpcCriterion.Create
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
    inner class GetAllUserCriteria {
        @Test
        fun `When all criteria of a specific user are requested, then the only criteria created by the according user are returned`() =
            runTest {
                val userId = createExampleUser("test@email.com")
                val projectId = createExampleProject().id

                val baseRequestBuilder = GrpcCriterion.Create.newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

                val userCriterionRequest = baseRequestBuilder.build()
                val projectCriterionRequest = baseRequestBuilder
                    .setProjectId(projectId.toString())
                    .build()

                val userCriterion1 = repo.createCriterion(userCriterionRequest, testUserId)
                val userCriterion2 = repo.createCriterion(userCriterionRequest, userId)
                val projectCriterion = repo.createCriterion(projectCriterionRequest, testUserId)

                val userCriteria = repo.getAllUserCriteria(testUserId) as List<Criterion>

                assertThat(userCriteria).hasSize(1)
                assertThat(userCriteria).containsExactly(userCriterion1)
                assertThat(userCriteria).doesNotContain(userCriterion2)
                assertThat(userCriteria).doesNotContain(projectCriterion)
            }
    }

    @Nested
    inner class GetCriteriaByIds {
        @Test
        fun `When all criteria of the given ids are requested and exist, then all those criteria are returned`() =
            runTest {
                val projectId = createExampleProject().id

                val baseRequestBuilder = GrpcCriterion.Create.newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

                val userCriterionRequest = baseRequestBuilder.build()
                val projectCriterionRequest = baseRequestBuilder
                    .setProjectId(projectId.toString())
                    .build()

                val userCriterion = repo.createCriterion(userCriterionRequest, testUserId)
                val projectCriterion = repo.createCriterion(projectCriterionRequest, testUserId)

                val userCriteria = repo.getCriteriaByIds(listOf(userCriterion.id, projectCriterion.id))

                assertThat(userCriteria).hasSize(2)
                assertThat(userCriteria).containsExactlyInAnyOrder(userCriterion, projectCriterion)
            }

        @Test
        fun `When all criteria of the given ids are requested but not all criteria exist, then only the existing criteria are returned`() =
            runTest {
                val baseRequestBuilder = GrpcCriterion.Create.newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

                val userCriterionRequest = baseRequestBuilder.build()

                val userCriterion = repo.createCriterion(userCriterionRequest, testUserId)

                val userCriteria = repo.getCriteriaByIds(listOf(userCriterion.id, UUID.randomUUID()))

                assertThat(userCriteria).hasSize(1)
                assertThat(userCriteria).containsExactlyInAnyOrder(userCriterion)
            }

        @Test
        fun `When all the input list is empty, then an empty list is returned`() = runTest {
            val userCriteria = repo.getCriteriaByIds(emptyList())
            assertThat(userCriteria).isEmpty()
        }

        @Test
        fun `When no criteria of the input list exist, then an empty list is returned`() = runTest {
            val userCriteria = repo.getCriteriaByIds(listOf(UUID.randomUUID()))
            assertThat(userCriteria).isEmpty()
        }

        @Test
        fun `When the input list contains duplicated ids, then every criterion is returned only once`() = runTest {
            val baseRequestBuilder = GrpcCriterion.Create.newBuilder()
                .setTag("Test Tag")
                .setName("Test Criterion")
                .setDescription("Test Description")
                .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

            val userCriterionRequest = baseRequestBuilder.build()

            val userCriterion = repo.createCriterion(userCriterionRequest, testUserId)

            val userCriteria = repo.getCriteriaByIds(listOf(userCriterion.id, userCriterion.id))

            assertThat(userCriteria).hasSize(1)
            assertThat(userCriteria).containsExactlyInAnyOrder(userCriterion)
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
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            val originalCriterion = repo.getCriterionById(criterionId).getOrThrow()

            val updatedCriterionDetails = originalCriterion.toGrpcCriterion().toBuilder()
                .setTag("Updated Tag")
                .setName("Updated Criterion")
                .setDescription("Updated Description")
                .setCategory(CriterionCategory.CRITERION_CATEGORY_INCLUSION)
                .build()

            val request = GrpcCriterion.Update.newBuilder()
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

    @Nested
    inner class GetAllProjectCriteria {
        @Test
        fun `When all criteria of a specific project are requested, then the only criteria associated with this project are returned`() =
            runTest {
                val projectId1 = createExampleProject().id
                val projectId2 = createExampleProject().id

                val baseRequestBuilder = GrpcCriterion.Create.newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

                val userCriterionRequest = baseRequestBuilder.build()
                val projectCriterionRequest1 = baseRequestBuilder
                    .setProjectId(projectId1.toString())
                    .build()
                val projectCriterionRequest2 = baseRequestBuilder
                    .setProjectId(projectId2.toString())
                    .build()

                val userCriterion = repo.createCriterion(userCriterionRequest, testUserId)
                val projectCriterion1 = repo.createCriterion(projectCriterionRequest1, testUserId)
                val projectCriterion2 = repo.createCriterion(projectCriterionRequest2, testUserId)

                val userCriteria = repo.getAllProjectCriteria(projectId1)

                assertThat(userCriteria).hasSize(1)
                assertThat(userCriteria).containsExactly(projectCriterion1)
                assertThat(userCriteria).doesNotContain(projectCriterion2)
                assertThat(userCriteria).doesNotContain(userCriterion)
            }
    }
}
