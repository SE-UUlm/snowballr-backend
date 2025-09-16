package se.uulm.snowballr.backend.service.project

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import snowballr.ProjectOuterClass.Project as GrpcProject

class UpdateProjectTest : MainServiceTest() {
    @ParameterizedTest
    @CsvSource(
        value = [
            "PROJECT_STATUS_ACTIVE",
            "PROJECT_STATUS_ACTIVE_LOCKED",
            "PROJECT_STATUS_ARCHIVED",
        ],
    )
    fun `When a server admin updates an existent project, then no exception is thrown`(statusName: String) = runTest {
        val status = ProjectStatus.valueOf(statusName)
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(status = status)
        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
        val request = GrpcProject.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.updateProject(request, project.status) } returns updatedProject

        assertDoesNotThrow { mainService.updateProject(request) }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "PROJECT_STATUS_ACTIVE",
            "PROJECT_STATUS_ACTIVE_LOCKED",
            "PROJECT_STATUS_ARCHIVED",
        ],
    )
    fun `When a project admin updates an existent project, then no exception is thrown`(statusName: String) = runTest {
        val status = ProjectStatus.valueOf(statusName)
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(status = status)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
        val request = GrpcProject.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.updateProject(request, project.status) } returns updatedProject

        assertDoesNotThrow { mainService.updateProject(request) }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "PROJECT_STATUS_ACTIVE",
            "PROJECT_STATUS_ACTIVE_LOCKED",
            "PROJECT_STATUS_ARCHIVED",
            "PROJECT_STATUS_DELETED",
        ],
    )
    fun `When a project member updates a project, then an UnauthorizedException is thrown`(statusName: String) =
        runTest {
            val status = ProjectStatus.valueOf(statusName)
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(status = status)
            val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
            val request = GrpcProject.Update
                .newBuilder()
                .setProject(updatedProject.toGrpcProject())
                .setMask(updateFieldMask)
                .build()

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

            assertThrows<UnauthorizedException.Single> { mainService.updateProject(request) }
        }

    @Test
    fun `When a server admin updates project to the project status DELETED, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val project = DataBuilder.createExampleProject(
                status = ProjectStatus.PROJECT_STATUS_ACTIVE,
            )
            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
                status = ProjectStatus.PROJECT_STATUS_DELETED,
            )

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
            val request = GrpcProject.Update
                .newBuilder()
                .setProject(updatedProject.toGrpcProject())
                .setMask(updateFieldMask)
                .build()

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

            assertThrows<FailedPreconditionException> { mainService.updateProject(request) }
        }

    @Test
    fun `When a project admin updates project to the project status DELETED, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(
                status = ProjectStatus.PROJECT_STATUS_ACTIVE,
            )
            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
                status = ProjectStatus.PROJECT_STATUS_DELETED,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
            val request = GrpcProject.Update
                .newBuilder()
                .setProject(updatedProject.toGrpcProject())
                .setMask(updateFieldMask)
                .build()

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

            assertThrows<FailedPreconditionException> { mainService.updateProject(request) }
        }

    @Test
    fun `When a server admin updates a deleted project, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(
            status = ProjectStatus.PROJECT_STATUS_DELETED,
        )
        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")
        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
        val request = GrpcProject.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<FailedPreconditionException> { mainService.updateProject(request) }
    }

    @Test
    fun `When a project admin updates a deleted project, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectStatus.PROJECT_STATUS_DELETED,
        )
        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
        val request = GrpcProject.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

        assertThrows<FailedPreconditionException> { mainService.updateProject(request) }
    }
}
