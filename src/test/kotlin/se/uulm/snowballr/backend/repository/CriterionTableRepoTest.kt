package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.criterion.Criterion.UserCriterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertCriterionAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.sql.SQLException
import java.util.UUID
import kotlin.test.assertIs

class CriterionTableRepoTest : RepositoryTest(arrayOf(CriterionTable, ProjectTable), true) {
    private val repo = CriterionTableRepo(db)

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
        fun `When a criterion is found, then a successful result with the correct criterion is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)

            val result = repo.getCriterionById(criterionId)

            val criterion = assertResultSuccess(result)
            assertIs<ProjectCriterion>(criterion)
            assertEquals(criterionId, criterion.id)
            assertEquals("Test Tag", criterion.tag)
            assertEquals("Test Criterion", criterion.name)
            assertEquals("Test Description", criterion.description)
            assertEquals(CriterionCategory.EXCLUSION, criterion.category)
            assertEquals(projectId, criterion.projectId)
        }

        @Test
        fun `When a criterion is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getCriterionById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class CreateCriterion {
        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a project criterion is created, then the values are correctly assigned`(category: CriterionCategory) =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val request = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = category,
                    projectId = projectId,
                )

                val criterion = repo.createCriterion(request, testUserId)

                assertIs<ProjectCriterion>(criterion)
                assertEquals("Test Tag", criterion.tag)
                assertEquals("Test Criterion", criterion.name)
                assertEquals("Test Description", criterion.description)
                assertEquals(category, criterion.category)
                assertEquals(projectId, criterion.projectId)
                assertEquals(testUserId, criterion.createdBy)
            }

        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a user criterion is created, then the values are correctly assigned`(category: CriterionCategory) =
            runTest {
                val request = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = category,
                    projectId = null,
                )

                val criterion = repo.createCriterion(request, testUserId)

                assertIs<UserCriterion>(criterion)
                assertEquals("Test Tag", criterion.tag)
                assertEquals("Test Criterion", criterion.name)
                assertEquals("Test Description", criterion.description)
                assertEquals(category, criterion.category)
                assertEquals(testUserId, criterion.createdBy)
            }

        @Test
        fun `When a project criterion is created, but the assigned project doesn't exist, then an SQLException is thrown`() =
            runTest {
                val request = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = CriterionCategory.EXCLUSION,
                    projectId = UUID.randomUUID(),
                )

                assertThrows<SQLException> { repo.createCriterion(request, testUserId) }
            }

        @Test
        fun `When two criteria are created, then they have different IDs`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val request = CreateCriterionRequest(
                tag = "Test Tag",
                name = "Test Criterion",
                description = "Test Description",
                category = CriterionCategory.EXCLUSION,
                projectId = projectId,
            )

            val criterion1 = repo.createCriterion(request, testUserId)
            val criterion2 = repo.createCriterion(request, testUserId)

            assertNotEquals(criterion2.id, criterion1.id)
        }

        @GrpcEnumSourceTest(CriterionCategory::class)
        fun `When a criterion is created, but the assigned user doesn't exist, then an SQLException is thrown`(
            category: CriterionCategory,
        ) = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val request = CreateCriterionRequest(
                tag = "Test Tag",
                name = "Test Criterion",
                description = "Test Description",
                category = category,
                projectId = projectId,
            )

            assertThrows<SQLException> { repo.createCriterion(request, UUID.randomUUID()) }
        }
    }

    @Nested
    inner class GetAllUserCriteria {
        @Test
        fun `When all criteria of a specific user are requested, then the only criteria created by the according user are returned`() =
            runTest {
                val userId = insertUserAndGetId("test@email.com")
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val baseRequest = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = CriterionCategory.EXCLUSION,
                    projectId = null,
                )
                val projectCriterionRequest = baseRequest.copy(projectId = projectId)

                val userCriterion1 = repo.createCriterion(baseRequest, testUserId)
                val userCriterion2 = repo.createCriterion(baseRequest, userId)
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
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val baseRequest = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = CriterionCategory.EXCLUSION,
                    projectId = null,
                )
                val projectCriterionRequest = baseRequest.copy(projectId = projectId)

                val userCriterion = repo.createCriterion(baseRequest, testUserId)
                val projectCriterion = repo.createCriterion(projectCriterionRequest, testUserId)

                val userCriteria = repo.getCriteriaByIds(listOf(userCriterion.id, projectCriterion.id))

                assertThat(userCriteria).hasSize(2)
                assertThat(userCriteria).containsExactlyInAnyOrder(userCriterion, projectCriterion)
            }

        @Test
        fun `When all criteria of the given ids are requested but not all criteria exist, then only the existent criteria are returned`() =
            runTest {
                val request = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = CriterionCategory.EXCLUSION,
                    projectId = null,
                )

                val userCriterion = repo.createCriterion(request, testUserId)

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
            val request = CreateCriterionRequest(
                tag = "Test Tag",
                name = "Test Criterion",
                description = "Test Description",
                category = CriterionCategory.EXCLUSION,
                projectId = null,
            )

            val userCriterion = repo.createCriterion(request, testUserId)

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
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            repo.getCriterionById(criterionId).getOrThrow()
            val request = UpdateCriterionRequest(
                criterionId = criterionId,
                tag = "Updated Tag",
                name = "Updated Criterion",
                description = "Updated Description",
                category = CriterionCategory.INCLUSION,
            )

            val updatedCriterion = repo.updateCriterion(request, fieldMask)

            if ("criterion.tag" in fieldMask) {
                assertEquals("Updated Tag", updatedCriterion.tag)
            } else {
                assertEquals("Test Tag", updatedCriterion.tag)
            }
            if ("criterion.name" in fieldMask) {
                assertEquals("Updated Criterion", updatedCriterion.name)
            } else {
                assertEquals("Test Criterion", updatedCriterion.name)
            }
            if ("criterion.description" in fieldMask) {
                assertEquals("Updated Description", updatedCriterion.description)
            } else {
                assertEquals("Test Description", updatedCriterion.description)
            }
            if ("criterion.category" in fieldMask) {
                assertEquals(CriterionCategory.INCLUSION, updatedCriterion.category)
            } else {
                assertEquals(CriterionCategory.EXCLUSION, updatedCriterion.category)
            }
        }
    }

    @Nested
    inner class DeleteCriteriaByIds {
        @Test
        fun `When a list of criteria IDs is given, then all matching criteria should be deleted`() = runTest {
            val criterionId1 = insertCriterionAndGetId(createdBy = testUserId)
            val criterionId2 = insertCriterionAndGetId(createdBy = testUserId)
            val criterionId3 = insertCriterionAndGetId(createdBy = testUserId)

            val idsToDelete = listOf(criterionId1, criterionId3)

            repo.deleteCriteriaByIds(idsToDelete)

            assertResultFailure<NotFoundException>(repo.getCriterionById(criterionId1))
            assertResultFailure<NotFoundException>(repo.getCriterionById(criterionId3))
            assertResultSuccess(repo.getCriterionById(criterionId2))
        }

        @Test
        fun `When an empty list of criteria IDs is given, then no criteria should be deleted`() = runTest {
            val criterionId1 = insertCriterionAndGetId(createdBy = testUserId)

            val idsToDelete = emptyList<UUID>()

            repo.deleteCriteriaByIds(idsToDelete)

            assertResultSuccess(repo.getCriterionById(criterionId1))
        }
    }

    @Nested
    inner class DeleteUserCriteriaByUserId {
        @Test
        fun `When a user ID is given, then all user criteria associated with this user should be deleted`() = runTest {
            val criterionId = insertCriterionAndGetId(createdBy = testUserId)

            repo.deleteUserCriteriaByUserId(testUserId)

            assertResultFailure<NotFoundException>(repo.getCriterionById(criterionId))
        }
    }

    @Nested
    inner class GetAllProjectCriteria {
        @Test
        fun `When all criteria of a specific project are requested, then the only criteria associated with this project are returned`() =
            runTest {
                val projectId1 = insertProjectAndGetId(createdBy = testUserId)
                val projectId2 = insertProjectAndGetId(createdBy = testUserId)
                val baseRequest = CreateCriterionRequest(
                    tag = "Test Tag",
                    name = "Test Criterion",
                    description = "Test Description",
                    category = CriterionCategory.EXCLUSION,
                    projectId = null,
                )
                val projectCriterionRequest1 = baseRequest.copy(projectId = projectId1)
                val projectCriterionRequest2 = baseRequest.copy(projectId = projectId2)

                val userCriterion = repo.createCriterion(baseRequest, testUserId)
                val projectCriterion1 = repo.createCriterion(projectCriterionRequest1, testUserId)
                val projectCriterion2 = repo.createCriterion(projectCriterionRequest2, testUserId)

                val projectCriteria = repo.getAllProjectCriteria(projectId1)

                assertThat(projectCriteria).hasSize(1)
                val projectCriterion = projectCriteria.first()
                assertEquals(projectCriterion1, projectCriterion)
                assertNotEquals(projectCriterion2, projectCriterion)
                assertNotEquals(userCriterion, projectCriterion)
            }
    }
}
