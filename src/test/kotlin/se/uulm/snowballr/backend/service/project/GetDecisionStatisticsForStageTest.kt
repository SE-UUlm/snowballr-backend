package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.notfound.StageNotFoundException
import snowballr.ProjectOuterClass.PaperDecision
import java.util.UUID
import kotlin.test.assertEquals
import snowballr.ProjectOuterClass.Project.Information as GrpcProjectInformation

class GetDecisionStatisticsForStageTest : ProjectServiceTest() {
    private fun getRequest() = GrpcProjectInformation.DecisionStatistics.Get
        .newBuilder()
        .setProjectId(UUID.randomUUID().toString())
        .setStage(0)

    @Test
    fun `When a user retrieves the decision statistics and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest()
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

        assertDoesNotThrow { service.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a user retrieves the decision statistics, but has no access, then an TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val request = getRequest()
                .setProjectId(project.id.toString())
                .build()

            mockCurrentUser(user)
            coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getDecisionStatisticsForStage(request) }
        }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest()
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a stage above the maximum is requested, then a StageNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(maxStage = 1)

        val request = getRequest()
            .setProjectId(project.id.toString())
            .setStage(project.maxStage + 1) // Request the first invalid stage
            .build()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<StageNotFoundException> { service.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When the highest valid stage is requested, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(maxStage = 1)

        val request = getRequest()
            .setProjectId(project.id.toString())
            .setStage(project.maxStage) // Request the last valid stage
            .build()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

        assertDoesNotThrow { service.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a valid stage is requested, then the correct statistics are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(maxStage = 1)
        val stage = 0L

        val paperCountsByDecision = mapOf(
            PaperDecision.PAPER_DECISION_ACCEPTED to 1,
            PaperDecision.PAPER_DECISION_DECLINED to 2,
            PaperDecision.PAPER_DECISION_IN_REVIEW to 3,
            PaperDecision.PAPER_DECISION_UNREVIEWED to 4,
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
                decision = PaperDecision.PAPER_DECISION_ACCEPTED,
            ),
            DataBuilder.createExampleProjectPaper(
                projectId = project.id,
                stage = stage + 1,
                decision = PaperDecision.PAPER_DECISION_DECLINED,
            ),
        )

        val request = getRequest()
            .setProjectId(project.id.toString())
            .setStage(stage)
            .build()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns
            projectPapers + papersInOtherStage

        val statistics = service.getDecisionStatisticsForStage(request)

        val statsByDecision = statistics.statisticsList.associateBy { it.decision }
        paperCountsByDecision.forEach { (decision, expectedCount) ->
            val actual = statsByDecision[decision]?.count ?: -1
            assertEquals(expectedCount, actual.toInt())
        }
    }
}
