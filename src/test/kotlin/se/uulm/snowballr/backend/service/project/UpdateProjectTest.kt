package se.uulm.snowballr.backend.service.project

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import snowballr.Fetcher
import snowballr.ProjectOuterClass.ProjectStatus
import kotlin.test.assertContains
import kotlin.test.assertIs
import snowballr.ProjectOuterClass.Project as GrpcProject

class UpdateProjectTest : ProjectServiceTest() {
    private fun getRequest(project: Project, paths: List<String>? = null) = GrpcProject.Update
        .newBuilder()
        .setProject(project.toGrpcProject())
        .also { if (paths != null) it.setMask(FieldMaskUtil.fromStringList(paths)) }
        .build()

    @Test
    fun `When a user updates a project, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(project)

        mockCurrentUser(user)
        coEvery {
            projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateProject(request) }
    }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(project)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.updateProject(request) }
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
        coEvery { projectRepoMock.updateProject(request) } returns project

        val result = service.updateProject(request)

        assertProjectEquality(project, result)
    }

    @Test
    fun `When a user updates a project to the project status DELETED, then a IllegalArgumentException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val updatedProject = project.copy(status = ProjectStatus.PROJECT_STATUS_DELETED)
            val request = getRequest(updatedProject, listOf("project.status"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<IllegalArgumentException> { service.updateProject(request) }
        }

    @Test
    fun `When a user updates a deleted project, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_DELETED)

        val updatedProject = project.copy(name = "Updated Name")
        val request = getRequest(updatedProject, listOf("project.name"))

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<FailedPreconditionException> { service.updateProject(request) }
    }

    @Test
    fun `When a user updates an archived project (not only status), then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)

            val updatedProject = project.copy(name = "Updated Name", status = ProjectStatus.PROJECT_STATUS_ACTIVE)
            val request = getRequest(updatedProject, listOf("project.name", "project.status"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> { service.updateProject(request) }
        }

    @ParameterizedTest
    @ValueSource(strings = ["PROJECT_STATUS_ACTIVE", "PROJECT_STATUS_ACTIVE_LOCKED"])
    fun `When a user updates an archived project (only active status), then the correct values are returned`(
        statusName: String,
    ) = runTest {
        val status = ProjectStatus.valueOf(statusName)
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)

        val updatedProject = project.copy(status = status)
        val request = getRequest(updatedProject, listOf("project.status"))

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns
            (updatedProject.status == ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)
        coEvery { projectRepoMock.updateProject(request) } returns updatedProject

        val result = service.updateProject(request)

        assertProjectEquality(updatedProject, result)
    }

    @Test
    fun `When a user updates an archived project (only archived status), then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)

            val updatedProject = project.copy(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)
            val request = getRequest(updatedProject, listOf("project.status"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.updateProject(request) } returns updatedProject

            val result = service.updateProject(request)

            assertProjectEquality(updatedProject, result)
        }

    @Test
    fun `When a user updates an archived project (only unsupported status), then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)

            val updatedProject = project.copy(status = ProjectStatus.PROJECT_STATUS_UNSPECIFIED)
            val request = getRequest(updatedProject, listOf("project.status"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> { service.updateProject(request) }
        }

    @Test
    fun `When a user updates an active locked project (project settings), then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

            val updatedProject = project.copy(reviewMaybeAllowed = false)
            val request = getRequest(updatedProject, listOf("project.settings.review_maybe_allowed"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> { service.updateProject(request) }
        }

    @Test
    fun `When a user updates an active locked project (not project settings), then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

            val updatedProject = project.copy(name = "Updated Name")
            val request = getRequest(updatedProject, listOf("project.name"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.isProjectLocked(project.id) } returns true
            coEvery { projectRepoMock.updateProject(request) } returns updatedProject

            val result = service.updateProject(request)

            assertProjectEquality(updatedProject, result)
        }

    @Test
    fun `When a user updates an active project, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE)

        val updatedProject = project.copy(name = "Updated Name")
        val request = getRequest(updatedProject, listOf("project.name"))

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
        coEvery { projectRepoMock.updateProject(request) } returns updatedProject

        val result = service.updateProject(request)

        assertProjectEquality(updatedProject, result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PROJECT_STATUS_UNSPECIFIED", "UNRECOGNIZED"])
    fun `When a user updates a project with unsupported status, then an IllegalStateException is thrown`(
        statusName: String,
    ) = runTest {
        val status = ProjectStatus.valueOf(statusName)
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = status)

        val updatedProject = project.copy(name = "Updated Name", status = ProjectStatus.PROJECT_STATUS_ACTIVE)
        val request = getRequest(updatedProject, listOf("project.name"))

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<IllegalStateException> { service.updateProject(request) }
    }

    @Test
    fun `When a user updates the fetchers of a project, then the fetchers are sanitized`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(fetchers = emptyMap())
        val availableFetchers = setOf(
            Fetcher.FetcherInformation.newBuilder()
                .setId("existent-fetcher")
                .putAllOptionsSchema(
                    mapOf(
                        "existent-option" to Fetcher.FetcherOptionSchema.getDefaultInstance(),
                    ),
                )
                .build(),
        )

        val fetchers = mapOf(
            "existent-fetcher" to mapOf(
                "existent-option" to "foo",
                "non-existent-option" to "bar",
            ),
            "non-existent-fetcher" to emptyMap(),
        )
        val updatedProject = project.copy(fetchers = fetchers)
        val request = getRequest(updatedProject, listOf("project.settings.fetchers"))

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
        coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers
        val finalRequest = slot<GrpcProject.Update>()
        coEvery { projectRepoMock.updateProject(capture(finalRequest)) } returns updatedProject

        service.updateProject(request)

        val sanitizedRequest = finalRequest.captured

        val updatedFetchers = sanitizedRequest.project.settings.fetchersMap
        assertContains(updatedFetchers.keys, "existent-fetcher")
        assertFalse(updatedFetchers.containsKey("non-existent-fetcher"))

        val existentFetcher = updatedFetchers["existent-fetcher"]
        assertIs<Fetcher.FetcherOptions>(existentFetcher)
        assertContains(existentFetcher.optionsMap.keys, "existent-option")
        assertFalse(existentFetcher.optionsMap.containsKey("non-existent-option"))
    }

    @Test
    fun `When a user updates the fetchers of a project and doesn't include a required option, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = emptyMap())
            val requiredOption = Fetcher.FetcherOptionSchema.newBuilder().setRequired(true).build()
            val availableFetchers = setOf(
                Fetcher.FetcherInformation.newBuilder()
                    .setId("fetcher")
                    .putAllOptionsSchema(
                        mapOf(
                            "option1" to requiredOption,
                        ),
                    )
                    .build(),
            )

            val fetchers = mapOf(
                "fetcher" to emptyMap<String, String>(),
            )
            val updatedProject = project.copy(fetchers = fetchers)
            val request = getRequest(updatedProject, listOf("project.settings.fetchers"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
            coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

            assertThrows<FailedPreconditionException> { service.updateProject(request) }
        }

    @Test
    fun `When a user updates the fetchers of a project and keeps a required option empty, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = emptyMap())
            val requiredOption = Fetcher.FetcherOptionSchema.newBuilder().setRequired(true).build()
            val availableFetchers = setOf(
                Fetcher.FetcherInformation.newBuilder()
                    .setId("fetcher")
                    .putAllOptionsSchema(
                        mapOf(
                            "option1" to requiredOption,
                        ),
                    )
                    .build(),
            )

            val fetchers = mapOf(
                "fetcher" to mapOf(
                    "option1" to "",
                ),
            )
            val updatedProject = project.copy(fetchers = fetchers)
            val request = getRequest(updatedProject, listOf("project.settings.fetchers"))

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, project.id, AccessType.UPDATE) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.isProjectLocked(project.id) } returns false
            coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

            assertThrows<FailedPreconditionException> { service.updateProject(request) }
        }
}
