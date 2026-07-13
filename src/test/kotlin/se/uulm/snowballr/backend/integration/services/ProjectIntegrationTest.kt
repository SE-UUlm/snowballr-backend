package se.uulm.snowballr.backend.integration.services

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
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

            val fetched = projectService.getProjectById(project.id)

            assertEquals("My Project", fetched.name)
            assertEquals(project.id, fetched.id)
        }

        @Test
        fun `When a user creates a project, then it appears in their active projects list`() = runTest {
            projectService.createProject(CreateProjectRequest(name = "Listed Project"))

            val userProjects = projectService.getAllProjectsForUser(testUserId)

            assertTrue(userProjects.any { it.name == "Listed Project" })
        }

        @Test
        fun `When a user creates multiple projects, then all of them appear in the active projects list`() = runTest {
            projectService.createProject(CreateProjectRequest(name = "Project Alpha"))
            projectService.createProject(CreateProjectRequest(name = "Project Beta"))

            val userProjects = projectService.getAllProjectsForUser(testUserId)
            val names = userProjects.map { it.name }

            assertTrue(names.contains("Project Alpha"))
            assertTrue(names.contains("Project Beta"))
        }
    }

    @Nested
    inner class UpdateProject {
        @Test
        fun `When an admin updates a project's name, then the updated name is persisted`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Original Name"))

            val updatedProject = project.copy(name = "Updated Name")
            val request = UpdateProjectRequest.fromProject(updatedProject)

            val result = projectService.updateProject(request, setOf("project.name"))

            assertEquals("Updated Name", result.name)

            val fetched = projectService.getProjectById(project.id)
            assertEquals("Updated Name", fetched.name)
        }

        @Test
        fun `When a project is archived, then it no longer appears in the active projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "To Archive"))

            val updatedProject = project.copy(status = ProjectStatus.ARCHIVED)
            val request = UpdateProjectRequest.fromProject(updatedProject)

            projectService.updateProject(request, setOf("project.status"))

            val activeProjects = projectService.getAllProjectsForUser(testUserId)
            assertFalse(activeProjects.any { it.id == project.id })
        }

        @Test
        fun `When a project is archived, then it appears in the archived projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Archived Project"))

            val updatedProject = project.copy(status = ProjectStatus.ARCHIVED)
            val request = UpdateProjectRequest.fromProject(updatedProject)

            projectService.updateProject(request, setOf("project.status"))

            val archivedProjects = projectService.getAllArchivedProjectsForUser(testUserId)
            assertTrue(archivedProjects.any { it.id == project.id })
        }

        @Test
        fun `When a user updates the fetchers of a project, then non-existent fetchers and options are removed`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Fetcher Project"))

                val availableFetchers = setOf(
                    FetcherInformationWithId(
                        id = "existent-fetcher",
                        information = DataBuilder.createExampleFetcherInformation(
                            optionSchema = mapOf(
                                "existent-option" to DataBuilder.createExampleFetcherOptionsSchema(),
                            ),
                        ),
                    ),
                )
                coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

                val updatedProject = project.copy(
                    fetchers = mapOf(
                        "existent-fetcher" to mapOf(
                            "existent-option" to "value1",
                            "non-existent-option" to "value2",
                        ),
                        "non-existent-fetcher" to emptyMap(),
                    ),
                )
                val request = UpdateProjectRequest.fromProject(updatedProject)

                val result = projectService.updateProject(request, setOf("project.settings.fetchers"))

                val fetchersMap = result.fetchers
                assertContains(fetchersMap.keys, "existent-fetcher")
                assertFalse(fetchersMap.containsKey("non-existent-fetcher"))
                val sanitizedOptions = assertNotNull(fetchersMap["existent-fetcher"])
                assertContains(sanitizedOptions.keys, "existent-option")
                assertFalse(sanitizedOptions.containsKey("non-existent-option"))
            }

        @Test
        fun `When a user updates the fetchers of a project and a required option is missing, then a FailedPreconditionException is thrown`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Required Option Project"))

                val requiredOption = DataBuilder.createExampleFetcherOptionsSchema(isRequired = true)
                val availableFetchers = setOf(
                    FetcherInformationWithId(
                        id = "fetcher",
                        information = DataBuilder.createExampleFetcherInformation(
                            optionSchema = mapOf(
                                "option1" to requiredOption,
                                "option2" to requiredOption,
                            ),
                        ),
                    ),
                )
                coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

                val updatedProject = project.copy(
                    fetchers = mapOf(
                        "fetcher" to mapOf("option1" to "value"),
                    ),
                )
                val request = UpdateProjectRequest.fromProject(updatedProject)

                assertThrows<FailedPreconditionException> {
                    projectService.updateProject(request, setOf("project.settings.fetchers"))
                }
            }
    }

    @Nested
    inner class DeleteProject {
        @Test
        fun `When an admin soft-deletes a project, then the operation succeeds`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "To Delete"))

            assertDoesNotThrow { projectService.softDeleteProject(project.id) }
        }

        @Test
        fun `When a project is soft-deleted, then it no longer appears in the active projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Deleted Project"))

            projectService.softDeleteProject(project.id)

            val activeProjects = projectService.getAllProjectsForUser(testUserId)
            assertFalse(activeProjects.any { it.id == project.id })
        }

        @Test
        fun `When a project is soft-deleted, then it appears in the deleted projects list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Soft Deleted"))

            projectService.softDeleteProject(project.id)

            val deletedProjects = projectService.getAllDeletedProjectsForUser(testUserId)
            assertTrue(deletedProjects.any { it.id == project.id })
        }
    }
}
