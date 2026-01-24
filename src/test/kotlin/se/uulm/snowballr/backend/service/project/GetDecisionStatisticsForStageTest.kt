package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.StageNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import kotlin.test.assertEquals
import snowballr.ProjectOuterClass.Project.Information as GrpcProjectInformation

class GetDecisionStatisticsForStageTest : MainServiceTest() {
    private val validRequestBuilder = GrpcProjectInformation.DecisionStatistics.Get
        .newBuilder()
        .setProjectId(UUID.randomUUID().toString())
        .setStage(0)

    @Test
    fun `When a server admin retrieves the decision statistics, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

        assertDoesNotThrow { mainService.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a project member retrieves the decision statistics, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

        assertDoesNotThrow { mainService.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a non member retrieves the decision statistics, then an UnauthorizedException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a nonexistent project is requested, then a ProjectNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns false

        assertThrows<ProjectNotFoundException> { mainService.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a stage above the maximum is requested, then a StageNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(maxStage = 1)

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .setStage(project.maxStage + 1) // Request the first invalid stage
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<StageNotFoundException> { mainService.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When the highest valid stage is requested, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(maxStage = 1)

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .setStage(project.maxStage) // Request the last valid stage
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns emptyList()

        assertDoesNotThrow { mainService.getDecisionStatisticsForStage(request) }
    }

    @Test
    fun `When a valid stage is requested, then the correct statistics are returned`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
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

        val request = validRequestBuilder
            .setProjectId(project.id.toString())
            .setStage(stage)
            .build()

        mockCurrentUser(user)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectPaperRepoMock.getAllProjectPapersForProject(project.id) } returns
            projectPapers + papersInOtherStage

        val statistics = assertDoesNotThrow { mainService.getDecisionStatisticsForStage(request) }

        val statsByDecision = statistics.statisticsList.associateBy { it.decision }
        paperCountsByDecision.forEach { (decision, expectedCount) ->
            val actual = statsByDecision[decision]?.count ?: -1
            assertEquals(
                expectedCount,
                actual.toInt(),
            )
        }
    }
}
