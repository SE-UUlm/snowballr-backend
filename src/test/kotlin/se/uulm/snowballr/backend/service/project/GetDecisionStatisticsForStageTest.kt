package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.exception.notfound.StageNotFoundException

class GetDecisionStatisticsForStageTest : ProjectServiceTest() {
    @Test
    fun `When a user retrieves the decision statistics and has access, then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

            val result = service.getDecisionStatisticsForStage(project.id, 0).statistics

            assertEquals(4, result.size)
            for (statistic in result) {
                assertEquals(0, statistic.count)
            }
        }

    @Test
    fun `When a user retrieves the decision statistics, but has no access, then an TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getDecisionStatisticsForStage(project.id, 0) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getDecisionStatisticsForStage(project.id, 0) }
    }

    @Test
    fun `When a stage above the maximum is requested, then a StageNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(maxStage = 1)

        val stage = project.maxStage + 1 // Request the first invalid stage

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<StageNotFoundException> { service.getDecisionStatisticsForStage(project.id, stage) }
    }

    @Test
    fun `When the highest valid stage is requested, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(maxStage = 1)

        val stage = project.maxStage // Request the last valid stage

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

        val result = service.getDecisionStatisticsForStage(project.id, stage).statistics

        assertEquals(4, result.size)
        for (statistic in result) {
            assertEquals(0, statistic.count)
        }
    }

    @Test
    fun `When a valid stage is requested, then the correct statistics are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(maxStage = 1)
        val stage = 0

        val paperCountsByDecision = mapOf(
            PaperDecision.ACCEPTED to 1,
            PaperDecision.DECLINED to 2,
            PaperDecision.IN_REVIEW to 3,
            PaperDecision.UNREVIEWED to 4,
        )

        val projectPapers = paperCountsByDecision.flatMap { (decision, count) ->
            (1..count).map {
                DataBuilder.createExampleProjectPaper(
                    projectId = project.id,
                    stage = stage,
                    decision = decision,
                )
            }
        }

        val papersInOtherStage = listOf(
            DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                stage = stage + 1,
                decision = PaperDecision.ACCEPTED,
            ),
            DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                stage = stage + 1,
                decision = PaperDecision.DECLINED,
            ),
        )

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns
            projectPapers + papersInOtherStage

        val statistics = service.getDecisionStatisticsForStage(project.id, stage)

        val statsByDecision = statistics.statistics.associateBy { it.decision }
        paperCountsByDecision.forEach { (decision, expectedCount) ->
            val actual = statsByDecision[decision]?.count ?: -1
            assertEquals(expectedCount, actual)
        }
    }
}
