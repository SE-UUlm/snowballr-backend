package se.uulm.snowballr.backend.service.project

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class UpdateProjectTest : MainServiceTest() {
    private val dummyUserUUID = UUID.randomUUID()

    @BeforeEach
    fun setupTest() {
        every { GrpcContext.getUserIdFromContext() } throws NotImplementedError()
        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { projectRepoMock.getProjectById(any()) } throws NotImplementedError()
        coEvery {
            projectMemberRepoMock.addUserToProject(any(), any())
        } throws NotImplementedError()
        coEvery {
            projectMemberRepoMock.promoteProjectMemberToAdmin(any(), any())
        } throws NotImplementedError()
        coEvery { projectMemberRepoMock.getAllProjectAdmins(any()) } throws NotImplementedError()
        coEvery { projectRepoMock.updateProject(any(), any()) } throws NotImplementedError()
    }

    @Test
    fun `When a server admin updates the project information successfully, then no exception is thrown`() =
        testCoroutine {
            val user = DataBuilder.createExampleUser(id = dummyUserUUID, role = UserRole.USER_ROLE_ADMIN)
            val project = DataBuilder.createExampleProject(
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
            )

            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED,
                similarityThreshold = 1F,
                snowballingType = ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD,
                reviewMaybeAllowed = false,
            )

            val updateFieldMask = FieldMaskUtil.fromStringList(
                listOf("name", "status", "similarityThreshold", "snowballingType", "reviewMaybeAllowed"),
            )
            val request = ProjectOuterClass.Project.Update.newBuilder().setProject(
                updatedProject.toGrpcProject(),
            ).setMask(updateFieldMask).build()

            every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
            coEvery { projectRepoMock.updateProject(request, false) } returns updatedProject

            assertDoesNotThrow { mainService.updateProject(request) }
        }

    @Test
    fun `When a project admin updates an ACITVE project, then no exception is thrown`() = testCoroutine {
        val user = DataBuilder.createExampleUser(id = dummyUserUUID, role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updatedProject = DataBuilder.createExampleProject(
            id = project.id,
            name = "Updated Project",
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED,
            similarityThreshold = 1F,
            snowballingType = ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD,
            reviewMaybeAllowed = false,
        )

        val updateFieldMask = FieldMaskUtil.fromStringList(
            listOf("name", "status", "similarityThreshold", "snowballingType", "reviewMaybeAllowed"),
        )
        val request = ProjectOuterClass.Project.Update.newBuilder().setProject(
            updatedProject.toGrpcProject(),
        ).setMask(updateFieldMask).build()

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.addUserToProject(dummyUserUUID, project.id) } returns projectMember
        coEvery { projectMemberRepoMock.promoteProjectMemberToAdmin(project.id, dummyUserUUID) } returns projectMember
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.updateProject(request, false) } returns updatedProject

        assertDoesNotThrow { mainService.updateProject(request) }
    }

    @Test
    fun `When a project member updates an ACTIVE project, then an unauthorized exception is thrown`() = testCoroutine {
        val user = DataBuilder.createExampleUser(id = dummyUserUUID, role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updatedProject = DataBuilder.createExampleProject(
            id = project.id,
            name = "Updated Project",
        )

        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
        val request = ProjectOuterClass.Project.Update.newBuilder().setProject(
            updatedProject.toGrpcProject(),
        ).setMask(updateFieldMask).build()

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.addUserToProject(dummyUserUUID, project.id) } returns
            projectMember
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.updateProject(request, false) } returns updatedProject

        assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateProject(request) }
    }

    @Test
    fun `When a project admin updates an ACITVE_LOCKED project, then no exception is thrown`() = testCoroutine {
        val user = DataBuilder.createExampleUser(id = dummyUserUUID, role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
        )
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val updatedProject = DataBuilder.createExampleProject(
            id = project.id,
            name = "Updated Project",
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED,
            similarityThreshold = 1F,
            snowballingType = ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD,
            reviewMaybeAllowed = false,
        )

        val updateFieldMask = FieldMaskUtil.fromStringList(
            listOf("name", "status"),
        )
        val request = ProjectOuterClass.Project.Update.newBuilder().setProject(
            updatedProject.toGrpcProject(),
        ).setMask(updateFieldMask).build()

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.addUserToProject(dummyUserUUID, project.id) } returns
            projectMember
        coEvery {
            projectMemberRepoMock.promoteProjectMemberToAdmin(project.id, dummyUserUUID)
        } returns projectMember
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.updateProject(request, true) } returns updatedProject

        assertDoesNotThrow { mainService.updateProject(request) }
    }

    @Test
    fun `When a project member updates an ACTIVE_LOCKED project, then an unauthorized exception is thrown`() =
        testCoroutine {
            val user = DataBuilder.createExampleUser(id = dummyUserUUID, role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
            )

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
            val request = ProjectOuterClass.Project.Update.newBuilder().setProject(
                updatedProject.toGrpcProject(),
            ).setMask(updateFieldMask).build()

            every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.addUserToProject(dummyUserUUID, project.id) } returns
                projectMember
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
            coEvery { projectRepoMock.updateProject(request, false) } returns updatedProject

            assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateProject(request) }
        }

    @Test
    fun `When a project admin updates an archived project, then a failed precondition exception is thrown`() =
        testCoroutine {
            val user = DataBuilder.createExampleUser(id = dummyUserUUID, role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            val updatedProject = DataBuilder.createExampleProject(
                id = project.id,
                name = "Updated Project",
            )

            val updateFieldMask = FieldMaskUtil.fromStringList(listOf("name"))
            val request = ProjectOuterClass.Project.Update.newBuilder().setProject(
                updatedProject.toGrpcProject(),
            ).setMask(updateFieldMask).build()

            every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.addUserToProject(dummyUserUUID, project.id) } returns
                projectMember
            coEvery {
                projectMemberRepoMock.promoteProjectMemberToAdmin(project.id, dummyUserUUID)
            } returns projectMember
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
            coEvery { projectRepoMock.updateProject(request, false) } returns updatedProject

            assertThrows<SnowballRException.FailedPreconditionException> { mainService.updateProject(request) }
        }

    @Test
    fun `When an error occurs while updating a project, then an exception is thrown`() = testCoroutine {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val updatedProject = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val request = ProjectOuterClass.Project.Update.newBuilder().setProject(updatedProject.toGrpcProject()).build()

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(any()) } returns user
        coEvery { projectRepoMock.getProjectById(updatedProject.id) } returns updatedProject
        coEvery { projectMemberRepoMock.getAllProjectAdmins(any()) } returns emptyList()
        coEvery { projectRepoMock.updateProject(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateProject(request) }
    }
}
