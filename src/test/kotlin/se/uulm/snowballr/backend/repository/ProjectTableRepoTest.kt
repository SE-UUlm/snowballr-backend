package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.assignUserToProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import java.sql.SQLException
import java.util.UUID

class ProjectTableRepoTest : RepositoryTest(arrayOf(ProjectTable, ProjectMemberTable), true) {
    private val repo = ProjectTableRepo(db)

    companion object {
        @JvmStatic
        fun validFieldMasks(): List<Arguments> = listOf(
            Arguments.of(listOf("project.name")),
            Arguments.of(listOf("project.status")),
            Arguments.of(listOf("project.settings.similarity_threshold")),
            Arguments.of(listOf("project.settings.snowballing_type")),
            Arguments.of(listOf("project.settings.review_maybe_allowed")),
        )
    }

    @Nested
    inner class GetProjectById {
        @Test
        fun `When a project is found, then a successful result with the correct project is returned`() = runTest {
            val projectId =
                insertProjectAndGetId("Test Project", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
            val result = repo.getProjectById(projectId)

            val project = assertResultSuccess(result)
            assertEquals(projectId, project.id)
            assertEquals("Test Project", project.name)
            assertEquals(ProjectStatus.PROJECT_STATUS_ACTIVE, project.status)
            assertEquals(0, project.currentStage)
            assertEquals(0, project.maxStage)
            assertEquals(0F, project.similarityThreshold)
            assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, project.snowballingType)
            assertTrue(project.reviewMaybeAllowed)
            assertEquals(ReviewDecisionMatrix.getDefaultInstance(), project.reviewDecisionMatrix)
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
            val projectId =
                insertProjectAndGetId("Test Project", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
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
            val projectBuilder = Project.Create.newBuilder().setName("Test Project").build()
            val project = repo.createProject(projectBuilder, testUserId, userSettings)

            assertEquals("Test Project", project.name)
            assertEquals(ProjectStatus.PROJECT_STATUS_ACTIVE, project.status)
            assertEquals(0, project.currentStage)
            assertEquals(0, project.maxStage)
            // Assert default settings from user
            assertEquals(0.5F, project.similarityThreshold)
            assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, project.snowballingType)
            assertFalse(project.reviewMaybeAllowed)
            assertEquals(ReviewDecisionMatrix.getDefaultInstance(), project.reviewDecisionMatrix)
            assertThat(project.fetchers).isEmpty()
        }

        @Test
        fun `When two projects are created, then they have different IDs`() = runTest {
            val userSettings = DataBuilder.createExampleUserSettings()
            val project = Project.Create.newBuilder().setName("Test Project 1").build()
            val projectId1 = repo.createProject(project, testUserId, userSettings)
            val projectId2 = repo.createProject(project, testUserId, userSettings)
            assertNotEquals(projectId2, projectId1)
        }

        @Test
        fun `When a project is created, but the assigned user doesn't exist, then an SQLException is thrown`() =
            runTest {
                val request = Project.Create.newBuilder().setName("Test Project").build()
                val userSettings = DataBuilder.createExampleUserSettings()
                assertThrows<SQLException> { repo.createProject(request, UUID.randomUUID(), userSettings) }
            }
    }

    @Nested
    inner class GetAllProjects {
        @Test
        fun `When projects are found, then all projects are returned`() = runTest {
            val project1Id =
                insertProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
            val project2Id =
                insertProjectAndGetId(
                    "Test Project 2",
                    ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
                    createdBy = testUserId,
                )

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(2)
            val firstProject = projects.find { it.id == project1Id }
            assertNotNull(firstProject)
            val secondProject = projects.find { it.id == project2Id }
            assertNotNull(secondProject)
        }

        @Test
        fun `When archived and deleted projects exist, then they are not returned`() = runTest {
            val project1Id =
                insertProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
            val project2Id =
                insertProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_ARCHIVED, createdBy = testUserId)
            val project3Id =
                insertProjectAndGetId("Test Project 3", ProjectStatus.PROJECT_STATUS_DELETED, createdBy = testUserId)

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
        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.ProjectTableRepoTest#validFieldMasks")
        fun `When a project is active and updated, then only the fields specified in the field mask are updated and the updated project is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val projectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE
            val projectId =
                insertProjectAndGetId(name = "Test Project", projectStatus, createdBy = testUserId)
            val originalProject = repo.getProjectById(projectId).getOrThrow()

            val updatedProjectDetails = originalProject.toGrpcProject().toBuilder()
                .setName("Updated Project")
                .setStatus(ProjectStatus.PROJECT_STATUS_ARCHIVED)
                .setSettings(
                    Project.Settings.newBuilder()
                        .setSimilarityThreshold(1F)
                        .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_FORWARD)
                        .setReviewMaybeAllowed(false)
                        .build(),
                )
                .build()

            val request = Project.Update.newBuilder()
                .setProject(updatedProjectDetails)
                .setMask(FieldMaskUtil.fromStringList(fieldMask))
                .build()

            val updatedProject = repo.updateProject(request, projectStatus)

            if ("project.name" in fieldMask) {
                assertEquals("Updated Project", updatedProject.name)
            } else {
                assertEquals("Test Project", updatedProject.name)
            }
            if ("project.status" in fieldMask) {
                assertEquals(ProjectStatus.PROJECT_STATUS_ARCHIVED, updatedProject.status)
            } else {
                assertEquals(projectStatus, updatedProject.status)
            }
            if ("project.settings.similarity_threshold" in fieldMask) {
                assertEquals(1F, updatedProject.similarityThreshold)
            } else {
                assertEquals(0F, updatedProject.similarityThreshold)
            }
            if ("project.settings.snowballing_type" in fieldMask) {
                assertEquals(SnowballingType.SNOWBALLING_TYPE_FORWARD, updatedProject.snowballingType)
            } else {
                assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, updatedProject.snowballingType)
            }
            if ("project.settings.review_maybe_allowed" in fieldMask) {
                assertFalse(updatedProject.reviewMaybeAllowed)
            } else {
                assertTrue(updatedProject.reviewMaybeAllowed)
            }
        }

        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.ProjectTableRepoTest#validFieldMasks")
        fun `When a project is active locked and updated, then only the project name and status are updated and the updated project is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val projectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
            val projectId =
                insertProjectAndGetId(name = "Test Project", projectStatus, createdBy = testUserId)
            val originalProject = repo.getProjectById(projectId).getOrThrow()

            val updatedProjectDetails = originalProject.toGrpcProject().toBuilder()
                .setName("Updated Project")
                .setStatus(ProjectStatus.PROJECT_STATUS_ARCHIVED)
                .setSettings(
                    Project.Settings.newBuilder()
                        .setSimilarityThreshold(1F)
                        .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_FORWARD)
                        .setReviewMaybeAllowed(false)
                        .build(),
                )
                .build()

            val request = Project.Update.newBuilder()
                .setProject(updatedProjectDetails)
                .setMask(FieldMaskUtil.fromStringList(fieldMask))
                .build()

            val updatedProject = repo.updateProject(request, projectStatus)

            if ("project.name" in fieldMask) {
                assertEquals("Updated Project", updatedProject.name)
            } else {
                assertEquals("Test Project", updatedProject.name)
            }
            if ("project.status" in fieldMask) {
                assertEquals(ProjectStatus.PROJECT_STATUS_ARCHIVED, updatedProject.status)
            } else {
                assertEquals(projectStatus, updatedProject.status)
            }
            assertEquals(0F, updatedProject.similarityThreshold)
            assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, updatedProject.snowballingType)
            assertTrue(updatedProject.reviewMaybeAllowed)
        }

        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.ProjectTableRepoTest#validFieldMasks")
        fun `When a project is archived and updated, then only the project status is updated and the updated project is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val projectStatus = ProjectStatus.PROJECT_STATUS_ARCHIVED
            val projectId =
                insertProjectAndGetId(name = "Test Project", projectStatus, createdBy = testUserId)
            val originalProject = repo.getProjectById(projectId).getOrThrow()

            val updatedProjectDetails = originalProject.toGrpcProject().toBuilder()
                .setName("Updated Project")
                .setStatus(ProjectStatus.PROJECT_STATUS_ACTIVE)
                .setSettings(
                    Project.Settings.newBuilder()
                        .setSimilarityThreshold(1F)
                        .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_FORWARD)
                        .setReviewMaybeAllowed(false)
                        .build(),
                )
                .build()

            val request = Project.Update.newBuilder()
                .setProject(updatedProjectDetails)
                .setMask(FieldMaskUtil.fromStringList(fieldMask))
                .build()

            val updatedProject = repo.updateProject(request, projectStatus)

            assertEquals("Test Project", updatedProject.name)
            if ("project.status" in fieldMask) {
                assertEquals(ProjectStatus.PROJECT_STATUS_ACTIVE, updatedProject.status)
            } else {
                assertEquals(projectStatus, updatedProject.status)
            }
            assertEquals(0F, updatedProject.similarityThreshold)
            assertEquals(SnowballingType.SNOWBALLING_TYPE_BOTH, updatedProject.snowballingType)
            assertTrue(updatedProject.reviewMaybeAllowed)
        }
    }

    @Nested
    inner class GetUserProjects {
        @Test
        fun `When active projects are found where the user is member of, then all (and only these) active projects are returned`() =
            runTest {
                val project1Id =
                    insertProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
                val project2Id =
                    insertProjectAndGetId(
                        "Test Project 2",
                        ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
                        createdBy = testUserId,
                    )
                val project3Id =
                    insertProjectAndGetId(
                        "Test Project 3",
                        ProjectStatus.PROJECT_STATUS_ARCHIVED,
                        createdBy = testUserId,
                    )
                val project4Id =
                    insertProjectAndGetId("Test Project 4", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)

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
                    insertProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
                val project2Id =
                    insertProjectAndGetId(
                        "Test Project 2",
                        ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
                        createdBy = testUserId,
                    )
                val project3Id =
                    insertProjectAndGetId(
                        "Test Project 3",
                        ProjectStatus.PROJECT_STATUS_ARCHIVED,
                        createdBy = testUserId,
                    )
                val project4Id =
                    insertProjectAndGetId(
                        "Test Project 4",
                        ProjectStatus.PROJECT_STATUS_ARCHIVED,
                        createdBy = testUserId,
                    )

                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val archivedUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))
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
                    insertProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE, createdBy = testUserId)
                val project2Id =
                    insertProjectAndGetId(
                        "Test Project 2",
                        ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
                        createdBy = testUserId,
                    )
                val project3Id =
                    insertProjectAndGetId(
                        "Test Project 3",
                        ProjectStatus.PROJECT_STATUS_DELETED,
                        createdBy = testUserId,
                    )
                val project4Id =
                    insertProjectAndGetId(
                        "Test Project 4",
                        ProjectStatus.PROJECT_STATUS_DELETED,
                        createdBy = testUserId,
                    )

                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val deletedUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.PROJECT_STATUS_DELETED))
                assertThat(deletedUserProjects).hasSize(1)

                assertNull(deletedUserProjects.find { it.id == project1Id })
                assertNull(deletedUserProjects.find { it.id == project2Id })
                assertNotNull(deletedUserProjects.find { it.id == project3Id })
                assertNull(deletedUserProjects.find { it.id == project4Id })
            }

        @Test
        fun `When no projects are found where the user is member of, then no projects are returned`() = runTest {
            val project1Id =
                insertProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ARCHIVED, createdBy = testUserId)
            val project2Id =
                insertProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_DELETED, createdBy = testUserId)

            val userId = insertUserAndGetId("userWithActiveProjects@example.com")

            assignUserToProject(userId, project1Id)
            assignUserToProject(userId, project2Id)

            val activeUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.PROJECT_STATUS_ACTIVE))
            assertThat(activeUserProjects).hasSize(0)

            assertNull(activeUserProjects.find { it.id == project1Id })
            assertNull(activeUserProjects.find { it.id == project2Id })
        }

        @Test
        fun `When an invalid project status is used to filter user projects, then an IllegalArgumentException is thrown`() =
            runTest {
                val userId = insertUserAndGetId("userWithActiveProjects@example.com")

                assertThrows<IllegalArgumentException> {
                    repo.getUserProjects(
                        userId,
                        setOf(ProjectStatus.PROJECT_STATUS_UNSPECIFIED),
                    )
                }
            }
    }
}
