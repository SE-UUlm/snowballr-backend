package se.uulm.snowballr.backend.service.project

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime
import java.util.UUID

class GetProjectInformationTest : ProjectServiceTest() {
    private fun getRequest(projectId: UUID, paths: List<String>? = null) = ProjectOuterClass.Project.Information.Get
        .newBuilder()
        .setProjectId(projectId.toString())
        .also { if (paths != null) it.setMask(FieldMaskUtil.fromStringList(paths)) }
        .build()

    @Test
    fun `When a user requests project information, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getProjectInformation(getRequest(project.id)) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getProjectInformation(getRequest(project.id)) }
    }

    @Test
    fun `When no field mask is given, then all fields are correctly returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val createdAt = OffsetDateTime.now()
        val stageStartedAt = OffsetDateTime.now()
        val project = DataBuilder.createExampleProject(
            createdAt = createdAt,
            currentStageStartedAt = stageStartedAt,
        )

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

        val response = service.getProjectInformation(getRequest(project.id))

        assertEquals(0.5f, response.projectProgress)
        assertEquals(createdAt.toEpochSecond(), response.creationDate.seconds)
        assertEquals(stageStartedAt.toEpochSecond(), response.lastStageStarted.seconds)
    }

    @ParameterizedTest
    @ValueSource(strings = ["project_progress", "creation_date", "last_stage_started"])
    fun `When a field mask is given with a path, then the specified fields are correctly returned`(path: String) =
        runTest {
            val user = DataBuilder.createExampleUser()
            val createdAt = OffsetDateTime.now()
            val stageStartedAt = OffsetDateTime.now()
            val project = DataBuilder.createExampleProject(
                createdAt = createdAt,
                currentStageStartedAt = stageStartedAt,
            )

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

            val response = service.getProjectInformation(getRequest(project.id, listOf(path)))

            if (path == "project_progress") {
                assertEquals(0.5f, response.projectProgress)
            } else {
                assertEquals(0f, response.projectProgress)
            }

            if (path == "creation_date") {
                assertEquals(createdAt.toEpochSecond(), response.creationDate.seconds)
            } else {
                assertEquals(0, response.creationDate.seconds)
            }

            if (path == "last_stage_started") {
                assertEquals(stageStartedAt.toEpochSecond(), response.lastStageStarted.seconds)
            } else {
                assertEquals(0, response.lastStageStarted.seconds)
            }
        }

    @Test
    fun `When a field mask is given without a path, then all fields are correctly returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val createdAt = OffsetDateTime.now()
        val stageStartedAt = OffsetDateTime.now()
        val project = DataBuilder.createExampleProject(
            createdAt = createdAt,
            currentStageStartedAt = stageStartedAt,
        )

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

        val response = service.getProjectInformation(getRequest(project.id, emptyList()))

        assertEquals(0.5f, response.projectProgress)
        assertEquals(createdAt.toEpochSecond(), response.creationDate.seconds)
        assertEquals(stageStartedAt.toEpochSecond(), response.lastStageStarted.seconds)
    }
}
