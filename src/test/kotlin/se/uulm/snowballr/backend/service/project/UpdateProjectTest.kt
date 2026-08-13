package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectField
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherOptions
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import kotlin.test.assertContains

class UpdateProjectTest : ProjectServiceTest() {
    private fun getRequest(project: Project) = UpdateProjectRequest.fromProject(project)

    @Test
    fun `When a user updates a project, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(project)

        mockCurrentUser(user)
        coEvery {
            projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateProject(request, emptySet()) }
    }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(project)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.updateProject(request, emptySet()) }
    }

    @Test
    fun `When a user updates a project and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(project)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
        coEvery { projectRepoMock.updateProject(request, emptySet()) } returns project

        val result = service.updateProject(request, emptySet())

        assertProjectEquality(project, result)
    }

    @Test
    fun `When a user updates a project to the project status DELETED, then a IllegalArgumentException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val updatedProject = project.copy(status = ProjectStatus.DELETED)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<IllegalArgumentException> { service.updateProject(request, setOf(ProjectField.STATUS)) }
        }

    @Test
    fun `When a user updates a deleted project, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.DELETED)

        val updatedProject = project.copy(name = "Updated Name")
        val request = getRequest(updatedProject)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<FailedPreconditionException> { service.updateProject(request, setOf(ProjectField.NAME)) }
    }

    @Test
    fun `When a user updates an archived project (not only status), then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ARCHIVED)

            val updatedProject = project.copy(name = "Updated Name", status = ProjectStatus.ACTIVE)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> {
                service.updateProject(request, setOf(ProjectField.NAME, ProjectField.STATUS))
            }
        }

    @ParameterizedTest
    @EnumSource(ProjectStatus::class, names = ["ACTIVE", "ACTIVE_LOCKED"])
    fun `When a user updates an archived project (only active status), then the correct values are returned`(
        status: ProjectStatus,
    ) = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.ARCHIVED)

        val updatedProject = project.copy(status = status)
        val request = getRequest(updatedProject)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns
            (updatedProject.status == ProjectStatus.ACTIVE_LOCKED)
        coEvery { projectRepoMock.updateProject(request, setOf(ProjectField.STATUS)) } returns updatedProject

        val result = service.updateProject(request, setOf(ProjectField.STATUS))

        assertProjectEquality(updatedProject, result)
    }

    @Test
    fun `When a user updates an archived project (only archived status), then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ARCHIVED)

            val updatedProject = project.copy(status = ProjectStatus.ARCHIVED)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.updateProject(request, setOf(ProjectField.STATUS)) } returns updatedProject

            val result = service.updateProject(request, setOf(ProjectField.STATUS))

            assertProjectEquality(updatedProject, result)
        }

    @Test
    fun `When a user updates an archived project (only unsupported status), then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ARCHIVED)

            val updatedProject = project.copy(status = ProjectStatus.CLEARED)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> { service.updateProject(request, setOf(ProjectField.STATUS)) }
        }

    @Test
    fun `When a user updates an active locked project (project settings), then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ACTIVE_LOCKED)

            val updatedProject = project.copy(reviewMaybeAllowed = false)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> {
                service.updateProject(request, setOf(ProjectField.REVIEW_MAYBE_ALLOWED))
            }
        }

    @Test
    fun `When a user updates an active locked project (not project settings), then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ACTIVE_LOCKED)

            val updatedProject = project.copy(name = "Updated Name")
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.isProjectLocked(project.id) } returns true
            coEvery { projectRepoMock.updateProject(request, setOf(ProjectField.NAME)) } returns updatedProject

            val result = service.updateProject(request, setOf(ProjectField.NAME))

            assertProjectEquality(updatedProject, result)
        }

    @Test
    fun `When a user updates an active project, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.ACTIVE)

        val updatedProject = project.copy(name = "Updated Name")
        val request = getRequest(updatedProject)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
        coEvery { projectRepoMock.updateProject(request, setOf(ProjectField.NAME)) } returns updatedProject

        val result = service.updateProject(request, setOf(ProjectField.NAME))

        assertProjectEquality(updatedProject, result)
    }

    @ParameterizedTest
    @EnumSource(ProjectStatus::class, names = ["CLEARED"])
    fun `When a user updates a project with unsupported status, then an IllegalStateException is thrown`(
        status: ProjectStatus,
    ) = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = status)

        val updatedProject = project.copy(name = "Updated Name", status = ProjectStatus.ACTIVE)
        val request = getRequest(updatedProject)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<IllegalStateException> { service.updateProject(request, setOf(ProjectField.NAME)) }
    }

    @Test
    fun `When a user updates the fetchers of a project, then the fetchers are sanitized`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(fetchers = emptyMap())
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

        val fetchers = mapOf(
            "existent-fetcher" to mapOf(
                "existent-option" to "foo",
                "non-existent-option" to "bar",
            ),
            "non-existent-fetcher" to emptyMap(),
        )
        val updatedProject = project.copy(fetchers = fetchers)
        val request = getRequest(updatedProject)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
        coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers
        val finalRequest = slot<UpdateProjectRequest>()
        coEvery {
            projectRepoMock.updateProject(capture(finalRequest), setOf(ProjectField.FETCHERS))
        } returns updatedProject

        service.updateProject(request, setOf(ProjectField.FETCHERS))

        val sanitizedRequest = finalRequest.captured

        val updatedFetchers = sanitizedRequest.settings.fetchers
        assertContains(updatedFetchers.keys, "existent-fetcher")
        assertFalse(updatedFetchers.containsKey("non-existent-fetcher"))

        val existentFetcher = updatedFetchers["existent-fetcher"]
        assertInstanceOf<FetcherOptions>(existentFetcher)
        assertContains(existentFetcher.keys, "existent-option")
        assertFalse(existentFetcher.containsKey("non-existent-option"))
    }

    @Test
    fun `When a user updates the fetchers of a project and doesn't include a required option, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = emptyMap())
            val availableFetchers = setOf(
                FetcherInformationWithId(
                    id = "fetcher",
                    information = DataBuilder.createExampleFetcherInformation(
                        optionSchema = mapOf(
                            "option1" to DataBuilder.createExampleFetcherOptionsSchema(isRequired = true),
                        ),
                    ),
                ),
            )

            val fetchers = mapOf(
                "fetcher" to emptyMap<String, String>(),
            )
            val updatedProject = project.copy(fetchers = fetchers)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
            coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

            assertThrows<FailedPreconditionException> {
                service.updateProject(request, setOf(ProjectField.FETCHERS))
            }
        }

    @Test
    fun `When a user updates the fetchers of a project and keeps a required option empty, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = emptyMap())
            val availableFetchers = setOf(
                FetcherInformationWithId(
                    id = "fetcher",
                    information = DataBuilder.createExampleFetcherInformation(
                        optionSchema = mapOf(
                            "option1" to DataBuilder.createExampleFetcherOptionsSchema(isRequired = true),
                        ),
                    ),
                ),
            )

            val fetchers = mapOf(
                "fetcher" to mapOf(
                    "option1" to "",
                ),
            )
            val updatedProject = project.copy(fetchers = fetchers)
            val request = getRequest(updatedProject)

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
            coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

            assertThrows<FailedPreconditionException> {
                service.updateProject(request, setOf(ProjectField.FETCHERS))
            }
        }
}
