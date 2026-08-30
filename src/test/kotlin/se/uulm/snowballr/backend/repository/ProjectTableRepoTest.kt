package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.isBetweenWithDelta
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.ProjectField
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectSettingRequest
import se.uulm.snowballr.backend.repository.RepositoryHelper.assignUserToProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertReviewAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID

private object ProjectDeleteBlockerTable : UUIDTable("project_delete_blocker") {
    val projectId = reference("project_id", ProjectTable, ReferenceOption.RESTRICT, ReferenceOption.RESTRICT)
}

class ProjectTableRepoTest :
    RepositoryTest(
        arrayOf(
            ProjectTable,
            ProjectMemberTable,
            ProjectPaperTable,
            PaperTable,
            ReviewTable,
            CriterionTable,
            ProjectDeleteBlockerTable,
        ),
        true,
    ) {
    private val repo = ProjectTableRepo(db)
    private val criterionRepo = CriterionTableRepo(db)

    private val defaultThresholdDate = OffsetDateTime.now().minusDays(30)

    @Nested
    inner class GetProjectById {
        @Test
        fun `When a project is found, then a successful result with the correct project is returned`() = runTest {
            val projectId =
                insertProjectAndGetId("Test Project", ProjectStatus.ACTIVE, createdBy = testUserId)
            val result = repo.getProjectById(projectId)

            val project = assertResultSuccess(result)

            assertEquals(projectId, project.id)
            assertEquals("Test Project", project.name)
            assertEquals(ProjectStatus.ACTIVE, project.status)
            assertEquals(0, project.currentStage)
            assertEquals(0, project.maxStage)
            assertEquals(0F, project.settings.similarityThreshold)
            assertEquals(SnowballingType.BOTH, project.settings.snowballingType)
            assertTrue(project.settings.reviewMaybeAllowed)
            assertEquals(ReviewDecisionMatrix(1, emptyList()), project.settings.reviewDecisionMatrix)
            assertEquals(testUserId, project.createdBy)
        }

        @Test
        fun `When a project is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getProjectById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class DoesProjectExistById {
        @Test
        fun `When a project with the given id exists, then true returned`() = runTest {
            val projectId = insertProjectAndGetId("Test Project", ProjectStatus.ACTIVE, createdBy = testUserId)
            val isProjectExistent = repo.doesProjectExistById(projectId)

            assertTrue(isProjectExistent)
        }

        @Test
        fun `When a project with the given id does not exist, then false returned`() = runTest {
            val projectId = UUID.randomUUID()
            val isProjectExistent = repo.doesProjectExistById(projectId)

            assertFalse(isProjectExistent)
        }
    }

    @Nested
    inner class CreateProject {
        @Test
        fun `When a project is created, then the passed values are correctly assigned`() = runTest {
            val userSettings = DataBuilder.createExampleUserSettings()
            val request = CreateProjectRequest(name = "Test Project")

            val project = repo.createProject(request, testUserId, userSettings)

            assertEquals("Test Project", project.name)
            assertEquals(ProjectStatus.ACTIVE, project.status)
            assertEquals(0, project.currentStage)
            assertEquals(0, project.maxStage)
            // Assert default settings from user
            assertEquals(0.5F, project.settings.similarityThreshold)
            assertEquals(SnowballingType.BOTH, project.settings.snowballingType)
            assertFalse(project.settings.reviewMaybeAllowed)
            assertEquals(ReviewDecisionMatrix(1, emptyList()), project.settings.reviewDecisionMatrix)
            assertThat(project.settings.fetchers).isEmpty()
        }

        @Test
        fun `When two projects are created, then they have different IDs`() = runTest {
            val userSettings = DataBuilder.createExampleUserSettings()
            val request = CreateProjectRequest(name = "Test Project 1")

            val projectId1 = repo.createProject(request, testUserId, userSettings)
            val projectId2 = repo.createProject(request, testUserId, userSettings)
            assertNotEquals(projectId2, projectId1)
        }

        @Test
        fun `When a project is created, but the assigned user doesn't exist, then an SQLException is thrown`() =
            runTest {
                val request = CreateProjectRequest(name = "Test Project")
                val userSettings = DataBuilder.createExampleUserSettings()

                assertThrows<SQLException> { repo.createProject(request, UUID.randomUUID(), userSettings) }
            }
    }

    @Nested
    inner class GetAllProjects {
        @Test
        fun `When projects are found, then all projects are returned`() = runTest {
            val project1Id = insertProjectAndGetId("Test Project 1", ProjectStatus.ACTIVE, createdBy = testUserId)
            val project2Id =
                insertProjectAndGetId("Test Project 2", ProjectStatus.ACTIVE_LOCKED, createdBy = testUserId)

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(2)
            val firstProject = projects.find { it.id == project1Id }
            assertNotNull(firstProject)
            val secondProject = projects.find { it.id == project2Id }
            assertNotNull(secondProject)
        }

        @Test
        fun `When archived and deleted projects exist, then they are not returned`() = runTest {
            val project1Id = insertProjectAndGetId("Test Project 1", ProjectStatus.ACTIVE, createdBy = testUserId)
            val project2Id = insertProjectAndGetId("Test Project 2", ProjectStatus.ARCHIVED, createdBy = testUserId)
            val project3Id = insertProjectAndGetId("Test Project 3", ProjectStatus.DELETED, createdBy = testUserId)

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(1)
            val firstProject = projects.find { it.id == project1Id }
            assertNotNull(firstProject)
            val secondProject = projects.find { it.id == project2Id }
            assertNull(secondProject)
            val thirdProject = projects.find { it.id == project3Id }
            assertNull(thirdProject)
        }
    }

    @Nested
    inner class UpdateProject {
        @ParameterizedTest(name = "Update the field {0}")
        @EnumSource(ProjectField::class)
        @Suppress("LongMethod", "CyclomaticComplexMethod")
        fun `When a project is updated, then only the specified field is updated`(field: ProjectField) = runTest {
            val originalStatus = ProjectStatus.ACTIVE
            val projectId = insertProjectAndGetId(name = "Test Project", originalStatus, createdBy = testUserId)
            val pattern = DecisionMatrixPattern(decision = PaperDecision.ACCEPTED, entries = emptyList())
            val request = UpdateProjectRequest(
                projectId = projectId,
                name = "Updated Project",
                status = ProjectStatus.ARCHIVED,
                settings = UpdateProjectSettingRequest(
                    similarityThreshold = 1F,
                    snowballingType = SnowballingType.FORWARD,
                    reviewMaybeAllowed = false,
                    fetchers = mapOf(
                        "test fetcher" to mapOf(
                            "Opt1" to "Val1",
                        ),
                    ),
                    decisionMatrix = ReviewDecisionMatrix(
                        numberOfReviewers = 2,
                        patterns = listOf(pattern),
                    ),
                ),
            )

            val start = OffsetDateTime.now()
            val updatedProject = repo.updateProject(request, setOf(field))
            val updatedSettings = updatedProject.settings
            val end = OffsetDateTime.now()

            val fetchers = updatedSettings.fetchers
            val fetcher = fetchers["test fetcher"]
            val excluded = ProjectField.entries.filter { field != it }

            // Included
            when (field) {
                ProjectField.NAME -> assertEquals("Updated Project", updatedProject.name)
                ProjectField.STATUS -> assertEquals(ProjectStatus.ARCHIVED, updatedProject.status)
                ProjectField.SIMILARITY_THRESHOLD -> assertEquals(1F, updatedSettings.similarityThreshold)
                ProjectField.SNOWBALLING_TYPE -> assertEquals(SnowballingType.FORWARD, updatedSettings.snowballingType)
                ProjectField.REVIEW_MAYBE_ALLOWED -> assertFalse(updatedSettings.reviewMaybeAllowed)
                ProjectField.FETCHERS -> {
                    assertEquals(1, fetchers.size)
                    assertNotNull(fetcher)
                    val fetcherOption1 = fetcher["Opt1"]
                    assertEquals("Val1", fetcherOption1)
                    val fetcherOption2 = fetcher["Opt2"]
                    assertNull(fetcherOption2)
                }
                ProjectField.NUMBER_OF_REVIEWERS ->
                    assertEquals(2, updatedSettings.reviewDecisionMatrix.numberOfReviewers)
                ProjectField.DECISION_MATRIX_PATTERNS -> {
                    assertEquals(1, updatedSettings.reviewDecisionMatrix.patterns.size)
                    assertThat(updatedSettings.reviewDecisionMatrix.patterns).containsOnly(pattern)
                }
            }

            // Excluded
            for (excludedField in excluded) {
                when (excludedField) {
                    ProjectField.NAME -> assertEquals("Test Project", updatedProject.name)
                    ProjectField.STATUS -> assertEquals(originalStatus, updatedProject.status)
                    ProjectField.SIMILARITY_THRESHOLD -> assertEquals(0F, updatedSettings.similarityThreshold)
                    ProjectField.SNOWBALLING_TYPE -> assertEquals(SnowballingType.BOTH, updatedSettings.snowballingType)
                    ProjectField.REVIEW_MAYBE_ALLOWED -> assertTrue(updatedSettings.reviewMaybeAllowed)
                    ProjectField.FETCHERS -> {
                        assertEquals(0, fetchers.size)
                        assertNull(fetcher)
                    }
                    ProjectField.NUMBER_OF_REVIEWERS ->
                        assertEquals(1, updatedSettings.reviewDecisionMatrix.numberOfReviewers)
                    ProjectField.DECISION_MATRIX_PATTERNS ->
                        assertEquals(0, updatedSettings.reviewDecisionMatrix.patterns.size)
                }
            }

            assertThat(updatedProject.modifiedAt).isBetweenWithDelta(start, end)
        }

        @Test
        fun `When only decision matrix patterns are updated, then number of reviewers remains unchanged`() = runTest {
            val initialDecisionMatrix = ReviewDecisionMatrix(
                numberOfReviewers = 3,
                patterns = listOf(DecisionMatrixPattern(PaperDecision.ACCEPTED, emptyList())),
            )
            val projectId = insertProjectAndGetId(
                name = "Decision Matrix Project",
                status = ProjectStatus.ACTIVE,
                reviewDecisionMatrix = initialDecisionMatrix,
                createdBy = testUserId,
            )
            val originalProject = repo.getProjectById(projectId).getOrThrow()
            val originalSettings = originalProject.settings

            val updatedDecisionMatrix = ReviewDecisionMatrix(
                numberOfReviewers = 9,
                patterns = listOf(DecisionMatrixPattern(PaperDecision.DECLINED, emptyList())),
            )
            val request = UpdateProjectRequest(
                projectId = originalProject.id,
                name = originalProject.name,
                status = originalProject.status,
                settings = UpdateProjectSettingRequest(
                    similarityThreshold = originalSettings.similarityThreshold,
                    snowballingType = originalSettings.snowballingType,
                    reviewMaybeAllowed = originalSettings.reviewMaybeAllowed,
                    fetchers = originalSettings.fetchers,
                    decisionMatrix = updatedDecisionMatrix,
                ),
            )

            val updatedProject = repo.updateProject(request, setOf(ProjectField.DECISION_MATRIX_PATTERNS))
            val updatedSettings = updatedProject.settings

            assertEquals(3, updatedSettings.reviewDecisionMatrix.numberOfReviewers)
            assertEquals(1, updatedSettings.reviewDecisionMatrix.patterns.size)
            assertEquals(PaperDecision.DECLINED, updatedSettings.reviewDecisionMatrix.patterns.first().decision)
        }

        @Test
        fun `When updating decision matrix on a non existing project, then NoSuchElementException is thrown`() =
            runTest {
                val request = UpdateProjectRequest(
                    projectId = UUID.randomUUID(),
                    name = "Missing Project",
                    status = ProjectStatus.ACTIVE,
                    settings = UpdateProjectSettingRequest(
                        similarityThreshold = 1F,
                        snowballingType = SnowballingType.BOTH,
                        reviewMaybeAllowed = false,
                        fetchers = emptyMap(),
                        decisionMatrix = ReviewDecisionMatrix(
                            numberOfReviewers = 2,
                            patterns = emptyList(),
                        ),
                    ),
                )

                assertThrows<NoSuchElementException> {
                    repo.updateProject(request, setOf(ProjectField.NUMBER_OF_REVIEWERS))
                }
            }

        @Test
        fun `When a project is updated without any specified fields, then nothing is updated`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val project = repo.getProjectById(projectId).getOrThrow()
            val request = UpdateProjectRequest.fromProject(project)

            val updatedProject = repo.updateProject(request, emptySet())

            assertEquals(project, updatedProject)
            assertNull(updatedProject.modifiedAt)
        }
    }

    @Nested
    inner class GetUserProjects {
        @Test
        fun `When active projects are found where the user is member of, then all (and only these) active projects are returned`() =
            runTest {
                val project1Id =
                    insertProjectAndGetId("Test Project 1", ProjectStatus.ACTIVE, createdBy = testUserId)
                val project2Id =
                    insertProjectAndGetId(
                        "Test Project 2",
                        ProjectStatus.ACTIVE_LOCKED,
                        createdBy = testUserId,
                    )
                val project3Id =
                    insertProjectAndGetId(
                        "Test Project 3",
                        ProjectStatus.ARCHIVED,
                        createdBy = testUserId,
                    )
                val project4Id = insertProjectAndGetId("Test Project 4", ProjectStatus.ACTIVE, createdBy = testUserId)

                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val activeUserProjects = repo.getUserProjects(userId)
                assertThat(activeUserProjects).hasSize(2)

                assertNotNull(activeUserProjects.find { it.id == project1Id })
                assertNotNull(activeUserProjects.find { it.id == project2Id })
                assertNull(activeUserProjects.find { it.id == project3Id })
                assertNull(activeUserProjects.find { it.id == project4Id })
            }

        @Test
        fun `When archived projects are found where the user is member of, then all (and only these) archived projects are returned`() =
            runTest {
                val project1Id =
                    insertProjectAndGetId("Test Project 1", ProjectStatus.ACTIVE, createdBy = testUserId)
                val project2Id =
                    insertProjectAndGetId(
                        "Test Project 2",
                        ProjectStatus.ACTIVE_LOCKED,
                        createdBy = testUserId,
                    )
                val project3Id =
                    insertProjectAndGetId(
                        "Test Project 3",
                        ProjectStatus.ARCHIVED,
                        createdBy = testUserId,
                    )
                val project4Id =
                    insertProjectAndGetId(
                        "Test Project 4",
                        ProjectStatus.ARCHIVED,
                        createdBy = testUserId,
                    )

                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val archivedUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.ARCHIVED))
                assertThat(archivedUserProjects).hasSize(1)

                assertNull(archivedUserProjects.find { it.id == project1Id })
                assertNull(archivedUserProjects.find { it.id == project2Id })
                assertNotNull(archivedUserProjects.find { it.id == project3Id })
                assertNull(archivedUserProjects.find { it.id == project4Id })
            }

        @Test
        fun `When deleted projects are found where the user is member of, then all (and only these) deleted projects are returned`() =
            runTest {
                val project1Id =
                    insertProjectAndGetId("Test Project 1", ProjectStatus.ACTIVE, createdBy = testUserId)
                val project2Id =
                    insertProjectAndGetId(
                        "Test Project 2",
                        ProjectStatus.ACTIVE_LOCKED,
                        createdBy = testUserId,
                    )
                val project3Id =
                    insertProjectAndGetId(
                        "Test Project 3",
                        ProjectStatus.DELETED,
                        createdBy = testUserId,
                    )
                val project4Id =
                    insertProjectAndGetId(
                        "Test Project 4",
                        ProjectStatus.DELETED,
                        createdBy = testUserId,
                    )

                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val deletedUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.DELETED))
                assertThat(deletedUserProjects).hasSize(1)

                assertNull(deletedUserProjects.find { it.id == project1Id })
                assertNull(deletedUserProjects.find { it.id == project2Id })
                assertNotNull(deletedUserProjects.find { it.id == project3Id })
                assertNull(deletedUserProjects.find { it.id == project4Id })
            }

        @Test
        fun `When no projects are found where the user is member of, then no projects are returned`() = runTest {
            val project1Id = insertProjectAndGetId("Test Project 1", ProjectStatus.ARCHIVED, createdBy = testUserId)
            val project2Id = insertProjectAndGetId("Test Project 2", ProjectStatus.DELETED, createdBy = testUserId)

            val userId = insertUserAndGetId("userWithActiveProjects@example.com")

            assignUserToProject(userId, project1Id)
            assignUserToProject(userId, project2Id)

            val activeUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.ACTIVE))
            assertThat(activeUserProjects).hasSize(0)

            assertNull(activeUserProjects.find { it.id == project1Id })
            assertNull(activeUserProjects.find { it.id == project2Id })
        }

        @Test
        fun `When an no project status is used to filter user projects, then an IllegalArgumentException is thrown`() =
            runTest {
                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assertThrows<IllegalArgumentException> {
                    repo.getUserProjects(userId, emptySet())
                }
            }
    }

    @Nested
    inner class IsProjectLocked {
        @Test
        fun `When a project has no project papers, then the project is not locked`() = runTest {
            val projectId = insertProjectAndGetId(status = ProjectStatus.ACTIVE, createdBy = testUserId)

            assertFalse(repo.isProjectLocked(projectId))
        }

        @Test
        fun `When a project has project papers without reviews, then the project is not locked`() = runTest {
            val projectId = insertProjectAndGetId(status = ProjectStatus.ACTIVE, createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)

            assertFalse(repo.isProjectLocked(projectId))
        }

        @Test
        fun `When a project has project papers with reviews, then the project is locked`() = runTest {
            val projectId = insertProjectAndGetId(status = ProjectStatus.ACTIVE, createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            insertReviewAndGetId(projectPaperId, userId = testUserId)

            assertTrue(repo.isProjectLocked(projectId))
        }
    }

    @Nested
    inner class SoftDeleteProject {
        @Test
        fun `When a project is soft-deleted, then it is marked as deleted`() = runTest {
            val projectId = insertProjectAndGetId(status = ProjectStatus.ACTIVE, createdBy = testUserId)

            val before = OffsetDateTime.now()
            repo.softDeleteProject(projectId)
            val after = OffsetDateTime.now()

            val deletedProject = repo.getProjectById(projectId).getOrThrow()

            assertTrue(deletedProject.status == ProjectStatus.DELETED)
            assertThat(deletedProject.deletedAt).isBetweenWithDelta(before, after)
        }

        @Test
        fun `When the project to be soft-deleted is not found, then no exception is thrown`() = runTest {
            val projectId = UUID.randomUUID()

            assertDoesNotThrow { repo.softDeleteProject(projectId) }
        }
    }

    @Nested
    inner class ClearSoftDeletedProjects {
        @Test
        fun `When no soft-deleted projects exist, then no project are cleared`() = runTest {
            val projectId = insertProjectAndGetId(status = ProjectStatus.ACTIVE, createdBy = testUserId)

            repo.clearSoftDeletedProjects(defaultThresholdDate)

            val project = assertResultSuccess(repo.getProjectById(projectId))
            assertEquals(ProjectStatus.ACTIVE, project.status)
            assertNull(project.deletedAt)
        }

        @Test
        fun `When soft-deleted projects exist but their threshold date is not reached, then no projects are cleared`() =
            runTest {
                val projectId = insertProjectAndGetId(status = ProjectStatus.ACTIVE, createdBy = testUserId)
                repo.softDeleteProject(projectId)

                repo.clearSoftDeletedProjects(defaultThresholdDate)

                val project = assertResultSuccess(repo.getProjectById(projectId))
                assertEquals(ProjectStatus.DELETED, project.status)
                assertNotNull(project.deletedAt)
                assertThat(project.deletedAt).isAfter(defaultThresholdDate)
                assertThat(project.name).isNotEmpty()
            }

        @Test
        fun `When soft-deleted projects exist and their threshold date is reached, then all soft-deleted projects are cleared`() =
            runTest {
                // Manually "soft-delete" project to set the `deletedAt` date
                val projectId1 = insertProjectAndGetId(
                    name = "Project 1",
                    status = ProjectStatus.DELETED,
                    deletedAt = defaultThresholdDate.minusDays(1),
                    createdBy = testUserId,
                )
                val projectId2 = insertProjectAndGetId(
                    name = "Project 2",
                    status = ProjectStatus.ACTIVE,
                    createdBy = testUserId,
                )

                repo.clearSoftDeletedProjects(defaultThresholdDate)

                val project1 = assertResultSuccess(repo.getProjectById(projectId1))
                assertEquals(ProjectStatus.CLEARED, project1.status)
                assertNotNull(project1.deletedAt)
                assertThat(project1.deletedAt).isBefore(defaultThresholdDate)
                assertThat(project1.name).isEmpty()

                val project2 = assertResultSuccess(repo.getProjectById(projectId2))
                assertEquals(ProjectStatus.ACTIVE, project2.status)
                assertNull(project2.deletedAt)
                assertThat(project2.name).isNotEmpty()
            }
    }

    @Nested
    inner class HardDeleteClearedProjects {
        @Test
        fun `When no cleared projects exist that have reached their threshold date to be cleared, then no projects are hard-deleted`() =
            runTest {
                val projectId1 = insertProjectAndGetId(name = "Project1", createdBy = testUserId)
                val projectId2 = insertProjectAndGetId(name = "Project2", createdBy = testUserId)
                repo.softDeleteProject(projectId1)

                repo.hardDeleteClearedProjects()

                val project1 = assertResultSuccess(repo.getProjectById(projectId1))
                assertEquals(ProjectStatus.DELETED, project1.status)
                assertNotNull(project1.deletedAt)
                assertThat(project1.name).isNotEmpty()

                val project2 = assertResultSuccess(repo.getProjectById(projectId2))
                assertEquals(ProjectStatus.ACTIVE, project2.status)
                assertNull(project2.deletedAt)
                assertThat(project2.name).isNotEmpty()
            }

        @Test
        fun `When cleared projects exist that have reached their threshold date to be cleared, then they are hard-deleted`() =
            runTest {
                // Manually "soft-delete" project to set the `deletedAt` date
                val projectId1 = insertProjectAndGetId(
                    name = "Project 1",
                    status = ProjectStatus.DELETED,
                    deletedAt = defaultThresholdDate.minusDays(1),
                    createdBy = testUserId,
                )
                val projectId2 = insertProjectAndGetId(name = "Project2", createdBy = testUserId)
                val criterionId1 = RepositoryHelper.insertCriterionAndGetId(
                    projectId = projectId1,
                    createdBy = testUserId,
                )
                val criterionId2 = RepositoryHelper.insertCriterionAndGetId(
                    projectId = projectId2,
                    createdBy = testUserId,
                )
                repo.clearSoftDeletedProjects(defaultThresholdDate)

                repo.hardDeleteClearedProjects()

                assertResultFailure<NotFoundException>(repo.getProjectById(projectId1))
                assertResultFailure<NotFoundException>(criterionRepo.getCriterionById(criterionId1))

                val project2 = assertResultSuccess(repo.getProjectById(projectId2))
                assertEquals(ProjectStatus.ACTIVE, project2.status)
                assertNull(project2.deletedAt)
                assertThat(project2.name).isNotEmpty()
                assertResultSuccess(criterionRepo.getCriterionById(criterionId2))
            }

        @Test
        fun `When a cleared project is still referenced by a restrict foreign key, then hard delete is skipped`() =
            runTest {
                val projectId = insertProjectAndGetId(
                    name = "Blocked Project",
                    status = ProjectStatus.DELETED,
                    deletedAt = defaultThresholdDate.minusDays(1),
                    createdBy = testUserId,
                )
                repo.clearSoftDeletedProjects(defaultThresholdDate)

                db.query {
                    ProjectDeleteBlockerTable.insert {
                        it[ProjectDeleteBlockerTable.projectId] = projectId
                    }
                }

                repo.hardDeleteClearedProjects()

                val project = assertResultSuccess(repo.getProjectById(projectId))
                assertEquals(ProjectStatus.CLEARED, project.status)
            }
    }

    @Nested
    inner class UpdateMaxStageIfExceeded {
        @Test
        fun `When the max stage is updated to a lower value, then the max stage stays the same`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId, maxStage = 12)

            repo.updateMaxStageIfExceeded(projectId, 9)
            val project = repo.getProjectById(projectId).getOrThrow()

            assertEquals(12, project.maxStage)
        }

        @Test
        fun `When the max stage is updated to a greater value, then the value is updated`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId, maxStage = 42)

            repo.updateMaxStageIfExceeded(projectId, 43)
            val project = repo.getProjectById(projectId).getOrThrow()

            assertEquals(43, project.maxStage)
        }

        @Test
        fun `When the max stage is updated to the equal value, then the max stage stays the same`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId, maxStage = 3)

            repo.updateMaxStageIfExceeded(projectId, 3)
            val project = repo.getProjectById(projectId).getOrThrow()

            assertEquals(3, project.maxStage)
        }
    }
}
