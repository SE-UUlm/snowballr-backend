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
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateProjectPaperException
import se.uulm.snowballr.backend.model.exception.invalidargument.StageOutOfRangeException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.MemberRole
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction
import snowballr.ProjectOuterClass.Project as GrpcProject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddPaperToProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val paperId = UUID.randomUUID()
    private fun getExampleRequest() = GrpcProject.Paper.Add.newBuilder()
        .setProjectId(projectId.toString())
        .setPaperId(paperId.toString())
        .setStage(0)
        .build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(paperRepoMock::getPaperById),
    )

    @Suppress("LongMethod", "ReturnCount", "CyclomaticComplexMethod")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(id = paperId, authors = listOf(author))
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(
            projectId = project.id,
            userId = currentUser.id,
            role = MemberRole.MEMBER_ROLE_ADMIN,
        )
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(currentUser)
        if (failAt == projectRepoMock::getProjectById) {
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.success(paper)

        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false
        coEvery {
            projectPaperRepoMock.addPaperToProject(getExampleRequest(), currentUser.id)
        } returns projectPaper
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
            mainService.addPaperToProject(getExampleRequest())
        }
    }

    @Test
    fun `When a server admin adds a paper to a project, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)
        assertDoesNotThrow { mainService.addPaperToProject(getExampleRequest()) }
    }

    @Test
    fun `When a project admin adds a paper to a project, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)
        assertDoesNotThrow { mainService.addPaperToProject(getExampleRequest()) }
    }

    @Test
    fun `When a non project admin adds a paper to a project, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.addPaperToProject(getExampleRequest()) }
    }

    @Test
    fun `When a project paper already exists, then a DuplicateProjectPaperException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = projectId)
        val paper = DataBuilder.createExamplePaper(id = paperId)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns true

        assertThrows<DuplicateProjectPaperException> {
            mainService.addPaperToProject(getExampleRequest())
        }
    }

    @Test
    fun `When the requested stage is greater than the projects max stage, then an StageOutOfRangeException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(id = projectId)
            val paper = DataBuilder.createExamplePaper(id = paperId)
            val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

            val request = GrpcProject.Paper.Add.newBuilder()
                .setProjectId(projectId.toString())
                .setPaperId(paperId.toString())
                .setStage(1)
                .build()

            mockCurrentUser(currentUser)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
            coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
            coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false

            assertThrows<StageOutOfRangeException> { mainService.addPaperToProject(request) }
        }
}
