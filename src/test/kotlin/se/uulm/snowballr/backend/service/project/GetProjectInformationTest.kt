package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.project.ProjectInfoField
import java.time.OffsetDateTime

class GetProjectInformationTest : ProjectServiceTest() {
    @Test
    fun `When a user requests project information, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getProjectInformation(project.id, emptySet()) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getProjectInformation(project.id, emptySet()) }
    }

    @Test
    fun `When no fields are specified, then all info fields are correctly returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val createdAt = OffsetDateTime.now()
        val stageStartedAt = OffsetDateTime.now()
        val project = DataBuilder.createExampleProject(
            createdAt = createdAt,
            currentStageStartedAt = stageStartedAt,
        )

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

        val response = service.getProjectInformation(project.id, emptySet())

        assertEquals(0.5f, response.progress)
        assertEquals(createdAt, response.creationDate)
        assertEquals(stageStartedAt, response.lastStageStarted)
    }

    @ParameterizedTest
    @EnumSource(ProjectInfoField::class)
    fun `When project info is requested, then only the specified field is filled with data`(field: ProjectInfoField) =
        runTest {
            val user = DataBuilder.createExampleUser()
            val createdAt = OffsetDateTime.now()
            val stageStartedAt = OffsetDateTime.now()
            val project = DataBuilder.createExampleProject(
                createdAt = createdAt,
                currentStageStartedAt = stageStartedAt,
            )

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            if (field == ProjectInfoField.PROJECT_PROGRESS) {
                coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f
            }

            val response = service.getProjectInformation(project.id, setOf(field))

            val excluded = ProjectInfoField.entries.filter { field != it }

            // Included
            when (field) {
                ProjectInfoField.PROJECT_PROGRESS -> assertEquals(0.5f, response.progress)
                ProjectInfoField.CREATION_DATE -> assertEquals(createdAt, response.creationDate)
                ProjectInfoField.LAST_STAGE_STARTED -> assertEquals(stageStartedAt, response.lastStageStarted)
            }

            // Excluded
            for (excludedField in excluded) {
                when (excludedField) {
                    ProjectInfoField.PROJECT_PROGRESS -> {
                        coVerify(exactly = 0) { projectPaperRepoMock.getProjectProgress(any()) }
                        assertEquals(0f, response.progress)
                    }

                    ProjectInfoField.CREATION_DATE -> assertEquals(OffsetDateTime.MIN, response.creationDate)
                    ProjectInfoField.LAST_STAGE_STARTED -> assertEquals(OffsetDateTime.MIN, response.lastStageStarted)
                }
            }
        }

    @Test
    fun `When project info is requested without any specified fields, then all fields are filled with data`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val createdAt = OffsetDateTime.now()
            val stageStartedAt = OffsetDateTime.now()
            val project = DataBuilder.createExampleProject(
                createdAt = createdAt,
                currentStageStartedAt = stageStartedAt,
            )

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectPaperRepoMock.getProjectProgress(project.id) } returns 0.5f

            val response = service.getProjectInformation(project.id, emptySet())

            assertEquals(0.5f, response.progress)
            assertEquals(createdAt, response.creationDate)
            assertEquals(stageStartedAt, response.lastStageStarted)
        }
}
