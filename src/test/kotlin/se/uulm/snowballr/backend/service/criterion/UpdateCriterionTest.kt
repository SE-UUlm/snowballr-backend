package se.uulm.snowballr.backend.service.criterion

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

class UpdateCriterionTest : MainServiceTest() {
    private val criterionId = UUID.randomUUID()

    private fun getExampleRequest(): GrpcCriterion.Update {
        val updatedCriterion = DataBuilder.createExampleProjectCriterion(
            id = criterionId,
            tag = "Updated Tag",
            name = "Updated Criterion",
            description = "Updated Description",
            category = CriterionCategory.CRITERION_CATEGORY_INCLUSION,
        )

        val updateFieldMask = FieldMaskUtil.fromStringList(
            listOf("tag", "name", "description", "category"),
        )

        return GrpcCriterion.Update.newBuilder()
            .setCriterion(updatedCriterion.toGrpcCriterion())
            .setMask(updateFieldMask)
            .build()
    }

    @Test
    fun `When a server admin updates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(
            status = ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = criterionId,
            projectId = project.id,
            createdBy = user.id,
        )

        val request = getExampleRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a project admin updates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = criterionId,
            projectId = project.id,
            createdBy = user.id,
        )

        val request = getExampleRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a project admin updates a project criterion of a non-active project, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(
                status = ProjectStatus.PROJECT_STATUS_ARCHIVED,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
            val criterion = DataBuilder.createExampleProjectCriterion(
                id = criterionId,
                projectId = project.id,
                createdBy = user.id,
            )

            val request = getExampleRequest()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

            assertThrows<FailedPreconditionException> { mainService.updateCriterion(request) }
        }

    @Test
    fun `When a project member updates a project criterion, then an UnauthorizedException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = ProjectStatus.PROJECT_STATUS_ACTIVE,
        )
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = criterionId,
            projectId = project.id,
            createdBy = user.id,
        )

        val request = getExampleRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a server admin updates a user criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleUserCriterion(id = criterionId, createdBy = UUID.randomUUID())

        val request = getExampleRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a user updates a user criterion, which he created himself, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val criterion =
            DataBuilder.createExampleUserCriterion(id = criterionId, createdBy = user.id)

        val request = getExampleRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)
        coEvery { criterionRepoMock.updateCriterion(request) } returns criterion

        assertDoesNotThrow { mainService.updateCriterion(request) }
    }

    @Test
    fun `When a user updates a user criterion, which he did not created himself, then an UnauthorizedException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion =
                DataBuilder.createExampleUserCriterion(id = criterionId, createdBy = UUID.randomUUID())

            val request = getExampleRequest()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

            assertThrows<UnauthorizedException> { mainService.updateCriterion(request) }
        }

    @Test
    fun `When an error occurs while the criterion is retrieved, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val request = getExampleRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.updateCriterion(request) }
    }
}
