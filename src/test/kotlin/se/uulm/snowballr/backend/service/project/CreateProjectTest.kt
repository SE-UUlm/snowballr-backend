package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import kotlin.test.assertEquals

class CreateProjectTest : MainServiceTest() {
    private fun getExampleRequest() = ProjectOuterClass.Project.Create.getDefaultInstance()

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        val request = getExampleRequest()
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createProject(request) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
        coVerify(exactly = 0) { userRepoMock.getUserSettings(any()) }
        coVerify(exactly = 0) { criterionRepoMock.getCriteriaByIds(any()) }
        coVerify(exactly = 0) { projectRepoMock.createProject(any(), any(), any()) }
        coVerify(exactly = 0) { criterionRepoMock.createCriterion(any(), any()) }
    }

    @Test
    fun `When an error occurs while a project is created, then an exception is thrown`() = runTest {
        val request = getExampleRequest()
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val userSettings = DataBuilder.createExampleUserSettings()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings
        coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()
        coEvery { projectRepoMock.createProject(request, user.id, userSettings) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createProject(request) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
        coVerify(exactly = 1) { userRepoMock.getUserSettings(user.id) }
        coVerify(exactly = 1) { criterionRepoMock.getCriteriaByIds(emptyList()) }
        coVerify(exactly = 1) { projectRepoMock.createProject(request, user.id, userSettings) }
        coVerify(exactly = 0) { criterionRepoMock.createCriterion(any(), any()) }
    }

    @Test
    fun `When a project is correctly created, then no exception is thrown`() = runTest {
        val request = getExampleRequest()
        val project = DataBuilder.createExampleProject()
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val userSettings = DataBuilder.createExampleUserSettings()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings
        coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()
        coEvery { projectRepoMock.createProject(request, user.id, userSettings) } returns project

        assertDoesNotThrow { mainService.createProject(request) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
        coVerify(exactly = 1) { userRepoMock.getUserSettings(user.id) }
        coVerify(exactly = 1) { criterionRepoMock.getCriteriaByIds(emptyList()) }
        coVerify(exactly = 1) { projectRepoMock.createProject(request, user.id, userSettings) }
        coVerify(exactly = 0) { criterionRepoMock.createCriterion(any(), any()) }
    }

    @Test
    fun `When a project is correctly created and the user has default criteria, then no exception is thrown`() =
        runTest {
            val request = getExampleRequest()
            val project = DataBuilder.createExampleProject()
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleUserCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))
            val criteriaIdsSlot = slot<List<UUID>>()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings
            coEvery { criterionRepoMock.getCriteriaByIds(capture(criteriaIdsSlot)) } returns listOf(criterion)
            coEvery { projectRepoMock.createProject(request, user.id, userSettings) } returns project
            coEvery { criterionRepoMock.createCriterion(any(), user.id) } returns criterion

            assertDoesNotThrow { mainService.createProject(request) }
            assertEquals(listOf(criterion.id), criteriaIdsSlot.captured)

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
            coVerify(exactly = 1) { userRepoMock.getUserSettings(user.id) }
            coVerify(exactly = 1) { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) }
            coVerify(exactly = 1) { projectRepoMock.createProject(request, user.id, userSettings) }
            coVerify(exactly = 1) { criterionRepoMock.createCriterion(any(), user.id) }
        }
}
