package se.uulm.snowballr.backend.service.projectpaper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.Project
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetProjectPaperByRelativeIdTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val localPaperId = Random.nextLong()

    private fun getExampleRequest() = Project.Paper.Get
        .newBuilder()
        .setProjectId(projectId.toString())
        .setRelativeProjectPaperId(localPaperId.toString())
        .build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectPaperRepoMock::getProjectPaperByRelativeId),
        Arguments.of(paperRepoMock::getPaperById),
    )

    @Suppress("LongMethod", "ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            localPaperId = localPaperId,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(currentUser)

        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true

        if (failAt == projectPaperRepoMock::getProjectPaperByRelativeId) {
            coEvery {
                projectPaperRepoMock.getProjectPaperByRelativeId(project.id, projectPaper.localPaperId)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, projectPaper.localPaperId)
        } returns Result.success(projectPaper)

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.success(paper)

        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt, true)

        assertThrows<TestSpecificException> {
            mainService.getProjectPaperByRelativeId(getExampleRequest())
        }
    }

    @Test
    fun `When a server admin retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)

        assertDoesNotThrow { mainService.getProjectPaperByRelativeId(getExampleRequest()) }
    }

    @Test
    fun `When a project member retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)

        assertDoesNotThrow { mainService.getProjectPaperByRelativeId(getExampleRequest()) }
    }

    @Test
    fun `When a non project member retrieves the project paper, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(projectId)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> {
            mainService.getProjectPaperByRelativeId(getExampleRequest())
        }
    }

    @Test
    fun `When a nonexistent project is requested, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns false

        assertThrows<NotFoundException> { mainService.getProjectPaperByRelativeId(getExampleRequest()) }
    }
}
