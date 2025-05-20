package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.ProjectOuterClass

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class CreateProjectTest : MainServiceTest() {
    @Test
    fun `When a project is correctly created, then no exception is thrown`() =
        testCoroutine {
            val request = ProjectOuterClass.Project.Create.getDefaultInstance()
            val project = ProjectOuterClass.Project.getDefaultInstance()

            coEvery { projectRepoMock.createProject(any()) } returns project

            assertDoesNotThrow { mainService.createProject(request) }
        }

    @Test
    fun `When an error occurs while a project is created, then an exception is thrown`() =
        testCoroutine {
            val request = ProjectOuterClass.Project.Create.getDefaultInstance()

            coEvery { projectRepoMock.createProject(any()) } throws Exception("Failed to create project")

            assertThrows<Exception> { mainService.createProject(request) }
        }
}
