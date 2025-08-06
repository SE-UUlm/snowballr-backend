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
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID

class GetProjectPaperByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestId.toString()).build()

    @Test
    fun `When retrieving current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested project paper fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested project members fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested paper by id fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested authors by paper id fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested backward references by paper id fails, then an exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val author = DataBuilder.createExampleAuthor()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a server admin retrieves the project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )
        val author = DataBuilder.createExampleAuthor()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a project member retrieves the project paper, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )
        val author = DataBuilder.createExampleAuthor()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns paper
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a non project member retrieves the project paper, then an unauthorized exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = requestId,
            projectId = project.id,
            paperId = paper.id,
        )

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns projectPaper
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> { mainService.getProjectPaperById(getExampleRequest()) }
    }
}
