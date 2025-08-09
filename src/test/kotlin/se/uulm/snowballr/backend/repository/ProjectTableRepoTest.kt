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
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.assignUserToProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.createExampleUser
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertTestProjectAndGetId
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import java.util.UUID

class ProjectTableRepoTest : RepositoryTest(arrayOf(ProjectTable, ProjectMemberTable), true) {
    private val repo = ProjectTableRepo(db)

    private suspend fun insertTestProjectAndGetId(name: String, status: ProjectStatus) =
        insertTestProjectAndGetId(name, status, testUserId)

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
        fun `When a project is found, then the correct project is returned`() = runTest {
            val projectId = insertTestProjectAndGetId("Test Project", ProjectStatus.PROJECT_STATUS_ACTIVE)
            val project = repo.getProjectById(projectId)

            assertThat(project.id).isEqualTo(projectId)
            assertThat(project.name).isEqualTo("Test Project")
            assertThat(project.status).isEqualTo(ProjectStatus.PROJECT_STATUS_ACTIVE)
            assertThat(project.currentStage).isEqualTo(0)
            assertThat(project.maxStage).isEqualTo(0)
            assertThat(project.similarityThreshold).isEqualTo(0F)
            assertThat(project.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            assertThat(project.reviewMaybeAllowed).isEqualTo(true)
            assertThat(project.reviewDecisionMatrix).isEqualTo(ReviewDecisionMatrix.getDefaultInstance())
            assertThat(project.createdBy).isEqualTo(testUserId)
        }

        @Test
        fun `When a project is not found, then an exception is thrown`() = runTest {
            assertThrows<NotFoundException> { repo.getProjectById(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class CreateProject {
        @Test
        fun `When a project is created, then the passed values are correctly assigned`() = runTest {
            val userSettings = DataBuilder.createExampleUserSettings()
            val projectBuilder = Project.Create.newBuilder().setName("Test Project").build()
            val project = repo.createProject(projectBuilder, testUserId, userSettings)

            assertThat(project.name).isEqualTo("Test Project")
            assertThat(project.status).isEqualTo(ProjectStatus.PROJECT_STATUS_ACTIVE)
            assertThat(project.currentStage).isEqualTo(0)
            assertThat(project.maxStage).isEqualTo(0)
            // Assert default settings from user
            assertThat(project.similarityThreshold).isEqualTo(0.5F)
            assertThat(project.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            assertThat(project.reviewMaybeAllowed).isFalse()
            assertThat(project.reviewDecisionMatrix).isEqualTo(ReviewDecisionMatrix.getDefaultInstance())
            assertThat(project.fetcherApis).isEmpty()
        }

        @Test
        fun `When two projects are created, then they have different IDs`() = runTest {
            val userSettings = DataBuilder.createExampleUserSettings()
            val project = Project.Create.newBuilder().setName("Test Project 1").build()
            val projectId1 = repo.createProject(project, testUserId, userSettings)
            val projectId2 = repo.createProject(project, testUserId, userSettings)
            assertThat(projectId1).isNotEqualTo(projectId2)
        }

        @Test
        fun `When a project is created, but the assigned user doesn't exist, then an exception is thrown`() = runTest {
            val request = Project.Create.newBuilder().setName("Test Project").build()
            val userSettings = DataBuilder.createExampleUserSettings()
            assertThrows<NotFoundException> { repo.createProject(request, UUID.randomUUID(), userSettings) }
        }
    }

    @Nested
    inner class GetAllProjects {
        @Test
        fun `When projects are found, then all projects are returned`() = runTest {
            val project1Id = insertTestProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
            val project2Id = insertTestProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(2)
            val firstProject = projects.find { it.id == project1Id }
            assertThat(firstProject).isNotNull
            val secondProject = projects.find { it.id == project2Id }
            assertThat(secondProject).isNotNull
        }

        @Test
        fun `When archived and deleted projects exist, then they are not returned`() = runTest {
            val project1Id = insertTestProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
            val project2Id = insertTestProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_ARCHIVED)
            val project3Id = insertTestProjectAndGetId("Test Project 3", ProjectStatus.PROJECT_STATUS_DELETED)

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(1)
            val firstProject = projects.find { it.id == project1Id }
            assertThat(firstProject).isNotNull
            val secondProject = projects.find { it.id == project2Id }
            assertThat(secondProject).isNull()
            val thirdProject = projects.find { it.id == project3Id }
            assertThat(thirdProject).isNull()
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
                insertTestProjectAndGetId(name = "Test Project", projectStatus)
            val originalProject = repo.getProjectById(projectId)

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
                assertThat(updatedProject.name).isEqualTo("Updated Project")
            } else {
                assertThat(updatedProject.name).isEqualTo("Test Project")
            }
            if ("project.status" in fieldMask) {
                assertThat(updatedProject.status).isEqualTo(ProjectStatus.PROJECT_STATUS_ARCHIVED)
            } else {
                assertThat(updatedProject.status).isEqualTo(projectStatus)
            }
            if ("project.settings.similarity_threshold" in fieldMask) {
                assertThat(updatedProject.similarityThreshold).isEqualTo(1F)
            } else {
                assertThat(updatedProject.similarityThreshold).isEqualTo(0F)
            }
            if ("project.settings.snowballing_type" in fieldMask) {
                assertThat(updatedProject.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_FORWARD)
            } else {
                assertThat(updatedProject.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            }
            if ("project.settings.review_maybe_allowed" in fieldMask) {
                assertThat(updatedProject.reviewMaybeAllowed).isEqualTo(false)
            } else {
                assertThat(updatedProject.reviewMaybeAllowed).isEqualTo(true)
            }
        }

        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.ProjectTableRepoTest#validFieldMasks")
        fun `When a project is active locked and updated, then only the project name and status are updated and the updated project is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val projectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
            val projectId =
                insertTestProjectAndGetId(name = "Test Project", projectStatus)
            val originalProject = repo.getProjectById(projectId)

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
                assertThat(updatedProject.name).isEqualTo("Updated Project")
            } else {
                assertThat(updatedProject.name).isEqualTo("Test Project")
            }
            if ("project.status" in fieldMask) {
                assertThat(updatedProject.status).isEqualTo(ProjectStatus.PROJECT_STATUS_ARCHIVED)
            } else {
                assertThat(updatedProject.status).isEqualTo(projectStatus)
            }
            assertThat(updatedProject.similarityThreshold).isEqualTo(0F)
            assertThat(updatedProject.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            assertThat(updatedProject.reviewMaybeAllowed).isEqualTo(true)
        }

        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.ProjectTableRepoTest#validFieldMasks")
        fun `When a project is archived and updated, then only the project status is updated and the updated project is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val projectStatus = ProjectStatus.PROJECT_STATUS_ARCHIVED
            val projectId =
                insertTestProjectAndGetId(name = "Test Project", projectStatus)
            val originalProject = repo.getProjectById(projectId)

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

            assertThat(updatedProject.name).isEqualTo("Test Project")
            if ("project.status" in fieldMask) {
                assertThat(updatedProject.status).isEqualTo(ProjectStatus.PROJECT_STATUS_ACTIVE)
            } else {
                assertThat(updatedProject.status).isEqualTo(projectStatus)
            }
            assertThat(updatedProject.similarityThreshold).isEqualTo(0F)
            assertThat(updatedProject.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            assertThat(updatedProject.reviewMaybeAllowed).isEqualTo(true)
        }
    }

    @Nested
    inner class GetUserProjects {
        @Test
        fun `When active projects are found where the user is member of, then all (and only these) active projects are returned`() =
            runTest {
                val project1Id = insertTestProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
                val project2Id = insertTestProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)
                val project3Id = insertTestProjectAndGetId("Test Project 3", ProjectStatus.PROJECT_STATUS_ARCHIVED)
                val project4Id = insertTestProjectAndGetId("Test Project 4", ProjectStatus.PROJECT_STATUS_ACTIVE)

                val userId = createExampleUser("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val activeUserProjects = repo.getUserProjects(userId)
                assertThat(activeUserProjects).hasSize(2)

                assertThat(activeUserProjects.find { it.id == project1Id }).isNotNull
                assertThat(activeUserProjects.find { it.id == project2Id }).isNotNull
                assertThat(activeUserProjects.find { it.id == project3Id }).isNull()
                assertThat(activeUserProjects.find { it.id == project4Id }).isNull()
            }

        @Test
        fun `When archived projects are found where the user is member of, then all (and only these) archived projects are returned`() =
            runTest {
                val project1Id = insertTestProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
                val project2Id = insertTestProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)
                val project3Id = insertTestProjectAndGetId("Test Project 3", ProjectStatus.PROJECT_STATUS_ARCHIVED)
                val project4Id = insertTestProjectAndGetId("Test Project 4", ProjectStatus.PROJECT_STATUS_ARCHIVED)

                val userId = createExampleUser("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val archivedUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))
                assertThat(archivedUserProjects).hasSize(1)

                assertThat(archivedUserProjects.find { it.id == project1Id }).isNull()
                assertThat(archivedUserProjects.find { it.id == project2Id }).isNull()
                assertThat(archivedUserProjects.find { it.id == project3Id }).isNotNull
                assertThat(archivedUserProjects.find { it.id == project4Id }).isNull()
            }

        @Test
        fun `When deleted projects are found where the user is member of, then all (and only these) deleted projects are returned`() =
            runTest {
                val project1Id = insertTestProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
                val project2Id = insertTestProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)
                val project3Id = insertTestProjectAndGetId("Test Project 3", ProjectStatus.PROJECT_STATUS_DELETED)
                val project4Id = insertTestProjectAndGetId("Test Project 4", ProjectStatus.PROJECT_STATUS_DELETED)

                val userId = createExampleUser("userWithActiveProjects@example.com")

                assignUserToProject(userId, project1Id)
                assignUserToProject(userId, project2Id)
                assignUserToProject(userId, project3Id)

                val deletedUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.PROJECT_STATUS_DELETED))
                assertThat(deletedUserProjects).hasSize(1)

                assertThat(deletedUserProjects.find { it.id == project1Id }).isNull()
                assertThat(deletedUserProjects.find { it.id == project2Id }).isNull()
                assertThat(deletedUserProjects.find { it.id == project3Id }).isNotNull
                assertThat(deletedUserProjects.find { it.id == project4Id }).isNull()
            }

        @Test
        fun `When no projects are found where the user is member of, then no projects are returned`() = runTest {
            val project1Id = insertTestProjectAndGetId("Test Project 1", ProjectStatus.PROJECT_STATUS_ARCHIVED)
            val project2Id = insertTestProjectAndGetId("Test Project 2", ProjectStatus.PROJECT_STATUS_DELETED)

            val userId = createExampleUser("userWithActiveProjects@example.com")

            assignUserToProject(userId, project1Id)
            assignUserToProject(userId, project2Id)

            val activeUserProjects = repo.getUserProjects(userId, setOf(ProjectStatus.PROJECT_STATUS_ACTIVE))
            assertThat(activeUserProjects).hasSize(0)

            assertThat(activeUserProjects.find { it.id == project1Id }).isNull()
            assertThat(activeUserProjects.find { it.id == project2Id }).isNull()
        }

        @Test
        fun `When an invalid project status is used to filter user projects, then an exception is thrown`() = runTest {
            val userId = createExampleUser("userWithActiveProjects@example.com")

            assertThrows<IllegalArgumentException> {
                repo.getUserProjects(
                    userId,
                    setOf(ProjectStatus.PROJECT_STATUS_UNSPECIFIED),
                )
            }
        }
    }
}
