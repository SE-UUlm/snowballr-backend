package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import java.util.UUID

class CreateProjectTest : MainServiceTest() {
    private fun getExampleRequest() = ProjectOuterClass.Project.Create.getDefaultInstance()

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createProject(getExampleRequest()) }
    }

    @Test
    fun `When an error occurs while a project is created, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { projectRepoMock.createProject(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createProject(getExampleRequest()) }
    }

    @Test
    fun `When a project is correctly created, then no exception is thrown`() = runTest {
        val project = DataBuilder.createExampleProject()

        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { projectRepoMock.createProject(any(), any()) } returns project

        assertDoesNotThrow { mainService.createProject(getExampleRequest()) }
    }
}
