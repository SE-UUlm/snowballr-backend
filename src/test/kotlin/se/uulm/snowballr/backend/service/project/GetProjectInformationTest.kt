package se.uulm.snowballr.backend.service.project

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime
import java.util.UUID

class GetProjectInformationTest : MainServiceTest() {
    private fun getRequest(projectId: UUID, paths: List<String>? = null) = ProjectOuterClass.Project.Information.Get
        .newBuilder()
        .setProjectId(projectId.toString())
        .also { if (paths != null) it.setMask(FieldMaskUtil.fromStringList(paths)) }
        .build()

    @Test
    fun `When the project does not exist, then a NotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectId = UUID.randomUUID()
        val member = DataBuilder.createExampleProjectMember(projectId = projectId, userId = user.id)

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns listOf(member)
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns false

        assertThrows<NotFoundException> { mainService.getProjectInformation(getRequest(projectId)) }
    }

    @Test
    fun `When the user is not a member of the project, then an UnauthorizedException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getProjectInformation(getRequest(project.id)) }
    }

    @Test
    fun `When the user is a member of the project and no field mask is given, then all fields are correctly returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val createdAt = OffsetDateTime.now()
            val stageStartedAt = OffsetDateTime.now()
            val project = DataBuilder.createExampleProject(
                createdAt = createdAt,
                currentStageStartedAt = stageStartedAt,
            )
            val member = DataBuilder.createExampleProjectMember(projectId = project.id, userId = user.id)

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(member)
            coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

            val response = mainService.getProjectInformation(getRequest(project.id))
            assertEquals(0.5f, response.projectProgress)
            assertEquals(createdAt.toEpochSecond(), response.creationDate.seconds)
            assertEquals(stageStartedAt.toEpochSecond(), response.lastStageStarted.seconds)
        }

    @Test
    fun `When the user is a member of the project and a field mask is given, then the specified fields are correctly returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val createdAt = OffsetDateTime.now()
            val stageStartedAt = OffsetDateTime.now()
            val project = DataBuilder.createExampleProject(
                createdAt = createdAt,
                currentStageStartedAt = stageStartedAt,
            )
            val member = DataBuilder.createExampleProjectMember(projectId = project.id, userId = user.id)

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(member)
            coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

            val response = mainService.getProjectInformation(getRequest(project.id, listOf("project_progress")))
            assertEquals(0.5f, response.projectProgress)
            assertEquals(0, response.creationDate.seconds)
            assertEquals(0, response.lastStageStarted.seconds)
        }
}
