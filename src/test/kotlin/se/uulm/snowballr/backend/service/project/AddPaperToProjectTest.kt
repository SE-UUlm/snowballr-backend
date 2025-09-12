package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
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
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddPaperToProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val paperId = UUID.randomUUID()
    private fun getExampleRequest() = ProjectOuterClass.Project.Paper.Add.newBuilder()
        .setProjectId(projectId.toString())
        .setPaperId(paperId.toString())
        .setStage(0)
        .build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(GrpcContext::getUserIdFromContext),
        Arguments.of(userRepoMock::getUserById),
        Arguments.of(projectMemberRepoMock::getProjectMembers),
        Arguments.of(projectRepoMock::getProjectById),
        Arguments.of(projectPaperRepoMock::doesProjectPaperExist),
        Arguments.of(paperRepoMock::getPaperById),
        Arguments.of(projectPaperRepoMock::addPaperToProject),
        Arguments.of(authorOfPaperRepoMock::getAuthorsOfPaperById),
        Arguments.of(citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById),
        Arguments.of(reviewRepoMock::getAllReviewsForProjectPaper),
        Arguments.of(reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById),
    )

    @Suppress("LongMethod", "ReturnCount", "CyclomaticComplexMethod")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserOuterClass.UserRole.USER_ROLE_ADMIN
            } else {
                UserOuterClass.UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val paper = DataBuilder.createExamplePaper(id = paperId)
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        if (failAt == GrpcContext::getUserIdFromContext) {
            every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
            return
        }
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id

        if (failAt == userRepoMock::getUserById) {
            coEvery { userRepoMock.getUserById(currentUser.id) } throws TestSpecificException()
            return
        }
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

        if (failAt == projectMemberRepoMock::getProjectMembers) {
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (failAt == projectRepoMock::getProjectById) {
            coEvery { projectRepoMock.getProjectById(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectRepoMock.getProjectById(project.id) } returns project

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } throws TestSpecificException()
            return
        }
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper

        if (failAt == projectPaperRepoMock::doesProjectPaperExist) {
            coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } throws TestSpecificException()
            return
        }
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false

        if (failAt == projectPaperRepoMock::addPaperToProject) {
            coEvery {
                projectPaperRepoMock.addPaperToProject(getExampleRequest(), currentUser.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            projectPaperRepoMock.addPaperToProject(getExampleRequest(), currentUser.id)
        } returns projectPaper

        if (failAt == authorOfPaperRepoMock::getAuthorsOfPaperById) {
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } throws TestSpecificException()
            return
        }
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)

        if (failAt == citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        if (failAt == reviewRepoMock::getAllReviewsForProjectPaper) {
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id)
            } throws TestSpecificException()
            return
        }
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)

        if (failAt == reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById) {
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(any())
            } throws TestSpecificException()
            return
        }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then an exception is thrown`(failAt: KFunction<*>) = runTest {
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
    fun `When a project member adds a paper to a project, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)
        assertDoesNotThrow { mainService.addPaperToProject(getExampleRequest()) }
    }

    @Test
    fun `When a non project member adds a paper to a project, then an unauthorized exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> { mainService.addPaperToProject(getExampleRequest()) }
    }

    @Test
    fun `When a project paper already exists, then a duplicate entity exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)
        val paper = DataBuilder.createExamplePaper(id = paperId)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { paperRepoMock.getPaperById(paper.id) } returns paper
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns true

        assertThrows<SnowballRException.DuplicateEntityException> {
            mainService.addPaperToProject(getExampleRequest())
        }
    }

    @Test
    fun `When the requested stage is greater than the projects max stage, then a out of range exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(id = projectId)
            val paper = DataBuilder.createExamplePaper(id = paperId)
            val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

            val request = ProjectOuterClass.Project.Paper.Add.newBuilder()
                .setProjectId(projectId.toString())
                .setPaperId(paperId.toString())
                .setStage(1)
                .build()

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { paperRepoMock.getPaperById(paper.id) } returns paper
            coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, paper.id) } returns false

            assertThrows<SnowballRException.OutOfRangeException> { mainService.addPaperToProject(request) }
        }
}
