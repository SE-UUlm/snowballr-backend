package se.uulm.snowballr.backend.service.project

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole

class UpdateProjectTest : MainServiceTest() {
    @ParameterizedTest
    @CsvSource(
        value = [
            "PROJECT_STATUS_ACTIVE",
            "PROJECT_STATUS_ACTIVE_LOCKED",
            "PROJECT_STATUS_ARCHIVED",
        ],
    )
    fun `When a server admin updates an existing project, then no exception is thrown`(statusName: String) = runTest {
        val status = ProjectOuterClass.ProjectStatus.valueOf(statusName)
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(status = status)
        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
        val request = ProjectOuterClass.Project.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
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
    fun `When a project admin updates an existing project, then no exception is thrown`(statusName: String) = runTest {
        val status = ProjectOuterClass.ProjectStatus.valueOf(statusName)
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(status = status)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
        val request = ProjectOuterClass.Project.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
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
    fun `When a project member updates a project, then an unauthorized exception is thrown`(statusName: String) =
        runTest {
            val status = ProjectOuterClass.ProjectStatus.valueOf(statusName)
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(status = status)
            val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
            val request = ProjectOuterClass.Project.Update
                .newBuilder()
                .setProject(updatedProject.toGrpcProject())
                .setMask(updateFieldMask)
                .build()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

            assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateProject(request) }
        }

    @Test
    fun `When a server admin updates project to the project status DELETED, then a failed precondition exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val project = DataBuilder.createExampleProject(
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
            )
            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED,
            )

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
            val request = ProjectOuterClass.Project.Update
                .newBuilder()
                .setProject(updatedProject.toGrpcProject())
                .setMask(updateFieldMask)
                .build()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

            assertThrows<SnowballRException.FailedPreconditionException> { mainService.updateProject(request) }
        }

    @Test
    fun `When a project admin updates project to the project status DELETED, then a failed precondition exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
            )
            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name", "status"))
            val request = ProjectOuterClass.Project.Update
                .newBuilder()
                .setProject(updatedProject.toGrpcProject())
                .setMask(updateFieldMask)
                .build()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

            assertThrows<SnowballRException.FailedPreconditionException> { mainService.updateProject(request) }
        }

    @Test
    fun `When a server admin updates a deleted project, then a failed precondition exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED,
        )
        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")
        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
        val request = ProjectOuterClass.Project.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<SnowballRException.FailedPreconditionException> { mainService.updateProject(request) }
    }

    @Test
    fun `When a project admin updates a deleted project, then a failed precondition exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED,
        )
        val updatedProject = DataBuilder.createExampleProject(id = project.id, name = "Updated Project")
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
        val request = ProjectOuterClass.Project.Update
            .newBuilder()
            .setProject(updatedProject.toGrpcProject())
            .setMask(updateFieldMask)
            .build()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

        assertThrows<SnowballRException.FailedPreconditionException> { mainService.updateProject(request) }
    }

    @Test
    fun `When an error occurs while updating a project, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
        val project = DataBuilder.createExampleProject(status = status)
        val request = ProjectOuterClass.Project.Update.newBuilder().setProject(project.toGrpcProject()).build()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(any()) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(any()) } returns emptyList()
        coEvery { projectRepoMock.updateProject(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateProject(request) }
    }
}
