package se.uulm.snowballr.backend.integration.services

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.Fetcher
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ProjectStatus
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectIntegrationTest : IntegrationTest() {
    @Nested
    inner class CreateAndRead {
        @Test
        fun `When a user creates a project, then it can be retrieved by ID`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "My Project"))
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            val fetched = projectService.getProjectById(projectId)

            assertEquals("My Project", fetched.name)
            assertEquals(project.id, fetched.id)
        }

        @Test
        fun `When a user creates a project, then it appears in their active projects list`() = runTest {
            projectService.createProject(CreateProjectRequest(name = "Listed Project"))

            val userProjects = projectService.getAllProjectsForUser(testUserId)

            assertTrue(userProjects.projectsList.any { it.name == "Listed Project" })
        }

        @Test
        fun `When a user creates multiple projects, then all of them appear in the active projects list`() = runTest {
            projectService.createProject(CreateProjectRequest(name = "Project Alpha"))
            projectService.createProject(CreateProjectRequest(name = "Project Beta"))

            val userProjects = projectService.getAllProjectsForUser(testUserId)
            val names = userProjects.projectsList.map { it.name }

            assertTrue(names.contains("Project Alpha"))
            assertTrue(names.contains("Project Beta"))
        }
    }

    @Nested
    inner class UpdateProject {
        @Test
        fun `When an admin updates a project's name, then the updated name is persisted`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Original Name"))
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            val updatedProject = Project.newBuilder().setId(project.id).setName("Updated Name").build()
            val request = Project.Update.newBuilder()
                .setProject(updatedProject)
                .setMask(FieldMaskUtil.fromStringList(listOf("project.name")))
                .build()

            val result = projectService.updateProject(request)

            assertEquals("Updated Name", result.name)

            val fetched = projectService.getProjectById(projectId)
            assertEquals("Updated Name", fetched.name)
        }

        @Test
        fun `When a project is archived, then it no longer appears in the active projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "To Archive"))

            val updatedProject = Project.newBuilder()
                .setId(project.id)
                .setStatus(ProjectStatus.PROJECT_STATUS_ARCHIVED)
                .build()
            val request = Project.Update.newBuilder()
                .setProject(updatedProject)
                .setMask(FieldMaskUtil.fromStringList(listOf("project.status")))
                .build()

            projectService.updateProject(request)

            val activeProjects = projectService.getAllProjectsForUser(testUserId)
            assertFalse(activeProjects.projectsList.any { it.id == project.id })
        }

        @Test
        fun `When a project is archived, then it appears in the archived projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Archived Project"))

            val updatedProject = Project.newBuilder()
                .setId(project.id)
                .setStatus(ProjectStatus.PROJECT_STATUS_ARCHIVED)
                .build()
            val request = Project.Update.newBuilder()
                .setProject(updatedProject)
                .setMask(FieldMaskUtil.fromStringList(listOf("project.status")))
                .build()

            projectService.updateProject(request)

            val archivedProjects = projectService.getAllArchivedProjectsForUser(testUserId)
            assertTrue(archivedProjects.projectsList.any { it.id == project.id })
        }

        @Test
        fun `When a user updates the fetchers of a project, then non-existent fetchers and options are removed`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Fetcher Project"))

                val availableFetchers = setOf(
                    Fetcher.FetcherInformation.newBuilder()
                        .setId("existent-fetcher")
                        .putOptionsSchema("existent-option", Fetcher.FetcherOptionSchema.getDefaultInstance())
                        .build(),
                )
                coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

                val fetcherOptions = Fetcher.FetcherOptions.newBuilder()
                    .putOptions("existent-option", "value1")
                    .putOptions("non-existent-option", "value2")
                    .build()
                val settings = Project.Settings.newBuilder()
                    .putFetchers("existent-fetcher", fetcherOptions)
                    .putFetchers("non-existent-fetcher", Fetcher.FetcherOptions.getDefaultInstance())
                    .build()
                val request = Project.Update.newBuilder()
                    .setProject(Project.newBuilder().setId(project.id).setSettings(settings).build())
                    .setMask(FieldMaskUtil.fromStringList(listOf("project.settings.fetchers")))
                    .build()

                val result = projectService.updateProject(request)

                val fetchersMap = result.settings.fetchersMap
                assertContains(fetchersMap.keys, "existent-fetcher")
                assertFalse(fetchersMap.containsKey("non-existent-fetcher"))
                val sanitizedOptions = assertNotNull(fetchersMap["existent-fetcher"])
                assertContains(sanitizedOptions.optionsMap.keys, "existent-option")
                assertFalse(sanitizedOptions.optionsMap.containsKey("non-existent-option"))
            }

        @Test
        fun `When a user updates the fetchers of a project and a required option is missing, then a FailedPreconditionException is thrown`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Required Option Project"))

                val requiredOption = Fetcher.FetcherOptionSchema.newBuilder().setRequired(true).build()
                val availableFetchers = setOf(
                    Fetcher.FetcherInformation.newBuilder()
                        .setId("fetcher")
                        .putOptionsSchema("option1", requiredOption)
                        .putOptionsSchema("option2", requiredOption)
                        .build(),
                )
                coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

                val fetcherOptions = Fetcher.FetcherOptions.newBuilder()
                    .putOptions("option1", "value")
                    .build()
                val settings = Project.Settings.newBuilder()
                    .putFetchers("fetcher", fetcherOptions)
                    .build()
                val request = Project.Update.newBuilder()
                    .setProject(Project.newBuilder().setId(project.id).setSettings(settings).build())
                    .setMask(FieldMaskUtil.fromStringList(listOf("project.settings.fetchers")))
                    .build()

                assertThrows<FailedPreconditionException> { projectService.updateProject(request) }
            }
    }

    @Nested
    inner class DeleteProject {
        @Test
        fun `When an admin soft-deletes a project, then the operation succeeds`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "To Delete"))
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            assertDoesNotThrow { projectService.softDeleteProject(projectId) }
        }

        @Test
        fun `When a project is soft-deleted, then it no longer appears in the active projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Deleted Project"))
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            projectService.softDeleteProject(projectId)

            val activeProjects = projectService.getAllProjectsForUser(testUserId)
            assertFalse(activeProjects.projectsList.any { it.id == project.id })
        }

        @Test
        fun `When a project is soft-deleted, then it appears in the deleted projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Soft Deleted"))
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            projectService.softDeleteProject(projectId)

            val deletedProjects = projectService.getAllDeletedProjectsForUser(testUserId)
            assertTrue(deletedProjects.projectsList.any { it.id == project.id })
        }
    }
}
