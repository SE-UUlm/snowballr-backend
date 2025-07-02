package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.ProjectOuterClass

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class CreateProjectTest : MainServiceTest() {
    @Test
    fun `When the requesting user has an invalid ID, then an exception is thrown`() = testCoroutine {
        val request = ProjectOuterClass.Project.Create.getDefaultInstance()

        dummyUserId = "invalid-UUID"

        assertThrows<InvalidIdException.UUID> { mainService.createProject(request) }
    }

    @Test
    fun `When a project is correctly created, then no exception is thrown`() = testCoroutine {
        val request = ProjectOuterClass.Project.Create.getDefaultInstance()
        val project = DataBuilder.createExampleProject()

        coEvery { projectRepoMock.createProject(any(), any()) } returns project

        assertDoesNotThrow { mainService.createProject(request) }
    }

    @Test
    fun `When an error occurs while a project is created, then an exception is thrown`() = testCoroutine {
        val request = ProjectOuterClass.Project.Create.getDefaultInstance()

        coEvery { projectRepoMock.createProject(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createProject(request) }
    }
}
