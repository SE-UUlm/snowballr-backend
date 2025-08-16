package se.uulm.snowballr.backend.service.criterion

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class UpdateCriterionTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest(): CriterionOuterClass.Criterion.Update {
        val updatedCriterion = DataBuilder.createExampleProjectCriterion(
            id = requestId,
            tag = "Updated Tag",
            name = "Updated Criterion",
            description = "Updated Description",
            category = CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION,
        )

        val updateFieldMask = FieldMaskUtil.fromStringList(
            listOf("tag", "name", "description", "category"),
        )

        return CriterionOuterClass.Criterion.Update.newBuilder()
            .setCriterion(updatedCriterion.toGrpcCriterion())
            .setMask(updateFieldMask)
            .build()
    }

    @Test
    fun `When a server admin updates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = requestId,
            projectId = project.id,
            createdBy = user.id,
        )

        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a project admin updates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = requestId,
            projectId = project.id,
            createdBy = user.id,
        )

        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a project admin updates a project criterion of a non-active project, then a failed precondition exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(
                status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
            val criterion = DataBuilder.createExampleProjectCriterion(
                id = requestId,
                projectId = project.id,
                createdBy = user.id,
            )

            val request = getExampleRequest()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

            assertThrows<SnowballRException.FailedPreconditionException> { mainService.updateCriterion(request) }
        }

    @Test
    fun `When a project member updates a project criterion, then an unauthorized exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = requestId,
            projectId = project.id,
            createdBy = user.id,
        )

        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a server admin updates a user criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion =
            DataBuilder.createExampleUserCriterion(id = requestId, createdBy = UUID.randomUUID())

        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a user updates a user criterion, which he created himself, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val criterion =
            DataBuilder.createExampleUserCriterion(id = requestId, createdBy = user.id)

        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a user updates a user criterion, which he did not created himself, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion =
                DataBuilder.createExampleUserCriterion(id = requestId, createdBy = UUID.randomUUID())

            val request = getExampleRequest()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion

            assertThrows<SnowballRException.UnauthorizedException> { mainService.updateCriterion(request) }
        }

    @Test
    fun `When an error occurs while the criterion is updated, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val criterion =
            DataBuilder.createExampleUserCriterion(id = requestId, createdBy = user.id)

        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { criterionRepoMock.updateCriterion(request) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateCriterion(request) }
    }
}
