package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.criterion.Criterion.UserCriterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.criterion.CriterionField
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.repository.RepositoryHelper.assignCriterionToReview
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertCriterionAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertReviewAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.sql.SQLException
import java.util.UUID

class CriterionTableRepoTest : RepositoryTest(
    arrayOf(
        ReviewHasCriterionTable,
        CriterionTable,
        ProjectTable,
        PaperTable,
        ProjectPaperTable,
        ReviewTable,
    ),
    true,
) {
    private val repo = CriterionTableRepo(db)

    private suspend fun insertReviewForProjectAndGetId(projectId: UUID): UUID {
        val paperId = insertPaperAndGetId()
        val projectPaperId = insertProjectPaperAndGetId(
            paperId = paperId,
            projectId = projectId,
            createdBy = testUserId,
        )
        return insertReviewAndGetId(projectPaperId, userId = testUserId)
    }

    @Nested
    inner class GetCriterionById {
        @Test
        fun `When a criterion is found, then a successful result with the correct criterion is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)

            val result = repo.getCriterionById(criterionId)

            val criterion = assertResultSuccess(result)
            assertInstanceOf<ProjectCriterion>(criterion)
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

                assertInstanceOf<ProjectCriterion>(criterion)
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

                assertInstanceOf<UserCriterion>(criterion)
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
        @ParameterizedTest(name = "Update the field {0}")
        @EnumSource(CriterionField::class)
        fun `When a criterion is updated, then only the specified field is updated`(field: CriterionField) = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            val request = UpdateCriterionRequest(
                criterionId = criterionId,
                tag = "Updated Tag",
                name = "Updated Criterion",
                description = "Updated Description",
                category = CriterionCategory.INCLUSION,
            )

            val updatedCriterion = repo.updateCriterion(request, setOf(field))

            val excluded = CriterionField.entries.filter { field != it }

            // Included
            when (field) {
                CriterionField.TAG -> assertEquals("Updated Tag", updatedCriterion.tag)
                CriterionField.NAME -> assertEquals("Updated Criterion", updatedCriterion.name)
                CriterionField.DESCRIPTION -> assertEquals("Updated Description", updatedCriterion.description)
                CriterionField.CATEGORY -> assertEquals(CriterionCategory.INCLUSION, updatedCriterion.category)
            }

            // Excluded
            for (excludedField in excluded) {
                when (excludedField) {
                    CriterionField.TAG -> assertEquals("Test Tag", updatedCriterion.tag)
                    CriterionField.NAME -> assertEquals("Test Criterion", updatedCriterion.name)
                    CriterionField.DESCRIPTION -> assertEquals("Test Description", updatedCriterion.description)
                    CriterionField.CATEGORY -> assertEquals(CriterionCategory.EXCLUSION, updatedCriterion.category)
                }
            }
        }

        @Test
        fun `When a criterion is updated without any specified fields, then nothing is updated`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            val criterion = repo.getCriterionById(criterionId).getOrThrow()
            val request = UpdateCriterionRequest.fromCriterion(criterion)

            val updatedCriterion = repo.updateCriterion(request, emptySet())

            assertEquals(criterion, updatedCriterion)
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
    inner class DeleteCriterionById {
        @Test
        fun `When a criterion ID is given, then only the matching criterion is deleted`() = runTest {
            val criterionId1 = insertCriterionAndGetId(createdBy = testUserId)
            val criterionId2 = insertCriterionAndGetId(createdBy = testUserId)

            repo.deleteCriterionById(criterionId1)

            assertResultFailure<NotFoundException>(repo.getCriterionById(criterionId1))
            assertResultSuccess(repo.getCriterionById(criterionId2))
        }

        @Test
        fun `When a non-existing criterion ID is given, then no criterion is deleted`() = runTest {
            val criterionId = insertCriterionAndGetId(createdBy = testUserId)

            repo.deleteCriterionById(UUID.randomUUID())

            assertResultSuccess(repo.getCriterionById(criterionId))
        }

        @Test
        fun `When a criterion associated with a review is deleted, then the association is deleted too`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val reviewId = insertReviewForProjectAndGetId(projectId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            assignCriterionToReview(reviewId, criterionId)

            repo.deleteCriterionById(criterionId)

            assertResultFailure<NotFoundException>(repo.getCriterionById(criterionId))
            assertFalse(repo.isAssociatedWithReviews(criterionId))
        }
    }

    @Nested
    inner class IsAssociatedWithReviews {
        @Test
        fun `When the criterion is associated with a review, then true is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val reviewId = insertReviewForProjectAndGetId(projectId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            assignCriterionToReview(reviewId, criterionId)

            assertTrue(repo.isAssociatedWithReviews(criterionId))
        }

        @Test
        fun `When the criterion is not associated with any review, then false is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val reviewId = insertReviewForProjectAndGetId(projectId)
            val criterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            val otherCriterionId = insertCriterionAndGetId(projectId = projectId, createdBy = testUserId)
            assignCriterionToReview(reviewId, otherCriterionId)

            assertFalse(repo.isAssociatedWithReviews(criterionId))
        }

        @Test
        fun `When the criterion does not exist, then false is returned`() = runTest {
            assertFalse(repo.isAssociatedWithReviews(UUID.randomUUID()))
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
