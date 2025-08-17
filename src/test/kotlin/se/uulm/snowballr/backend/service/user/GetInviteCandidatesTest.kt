package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.Project.InviteCandidatesRequest
import java.util.UUID
import kotlin.test.assertTrue

class GetInviteCandidatesTest : MainServiceTest() {
    private val testProjectId = UUID.randomUUID()
    private val validInviteCandidatesRequest = InviteCandidatesRequest.newBuilder().setQuery(
        "john",
    ).setProjectId(testProjectId.toString()).build()

    @Test
    fun `When the search query is too short, then an empty list is returned`() = runTest {
        val shortSearchQuery = InviteCandidatesRequest.newBuilder().setQuery("j").build()

        val result = mainService.getInviteCandidates(shortSearchQuery)
        assertThat(result.usersList).isEmpty()

        verify(exactly = 0) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
        coVerify(exactly = 0) { projectMemberRepoMock.getProjectMembers(any()) }
        coVerify(exactly = 0) { userRepoMock.getUsersMatchingSearchQuery(any(), any()) }
    }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = runTest {
        val userId = UUID.randomUUID()
        every { GrpcContext.getUserIdFromContext() } returns userId
        coEvery { userRepoMock.getUserById(userId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getInviteCandidates(validInviteCandidatesRequest) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(userId) }
        coVerify(exactly = 0) { projectMemberRepoMock.getProjectMembers(any()) }
        coVerify(exactly = 0) { userRepoMock.getUsersMatchingSearchQuery(any(), any()) }
    }

    @Test
    fun `When parsing the project id fails, then only a warning is logged`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestWithInvalidProjectId = InviteCandidatesRequest.newBuilder().setQuery(
            "john",
        ).setProjectId("invalid-uuid").build()
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(requestWithInvalidProjectId) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 0) { projectMemberRepoMock.getProjectMembers(any()) }
        coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery("john", setOf(currentUser.id)) }
    }

    @Test
    fun `When no project members exist, then no users except for the current user are excluded`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedProjectId = UUID.randomUUID()
        val requestWithNotExistingProject = InviteCandidatesRequest.newBuilder().setQuery(
            "john",
        ).setProjectId(requestedProjectId.toString()).build()
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectMemberRepoMock.getProjectMembers(requestedProjectId) } returns emptyList()
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(requestWithNotExistingProject) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(requestedProjectId) }
        coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery("john", setOf(currentUser.id)) }
    }

    @Test
    fun `When retrieving the users matching the search query returns no users, a successful call is made`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectMemberRepoMock.getProjectMembers(testProjectId) } returns emptyList()
        coEvery { userRepoMock.getUsersMatchingSearchQuery("john", setOf(currentUser.id)) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(validInviteCandidatesRequest) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(testProjectId) }
        coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery("john", setOf(currentUser.id)) }
    }

    @Test
    fun `When retrieving the invite candidates is successful, then these are returned except for the current user`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = "current.user@example.com")
            val otherUser = DataBuilder.createExampleUser(email = "another.user@example.com")
            val users = listOf(currentUser, otherUser)
            val excludedUsers = setOf(currentUser.id)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { projectMemberRepoMock.getProjectMembers(testProjectId) } returns emptyList()
            coEvery {
                userRepoMock.getUsersMatchingSearchQuery("john", excludedUsers)
            } returns users.filterNot { it.id in excludedUsers }

            val inviteCandidates = mainService.getInviteCandidates(validInviteCandidatesRequest)
            assertTrue { inviteCandidates.usersList.size == 1 }
            assertTrue { inviteCandidates.usersList.find { it.id == currentUser.id.toString() } == null }

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
            coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(testProjectId) }
            coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery("john", excludedUsers) }
        }

    @Test
    fun `When retrieving the project members is successful, then these members are not returned as invite candidates`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = "current.user@example.com")
            val projectMemberUser = DataBuilder.createExampleUser(email = "project.member@example.com")
            val projectMember =
                DataBuilder.createExampleProjectMember(userId = projectMemberUser.id, projectId = testProjectId)
            val excludedUsers = setOf(currentUser.id, projectMemberUser.id)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { projectMemberRepoMock.getProjectMembers(testProjectId) } returns listOf(projectMember)
            coEvery { userRepoMock.getUsersMatchingSearchQuery("john", excludedUsers) } returns emptyList()

            val inviteCandidates = mainService.getInviteCandidates(validInviteCandidatesRequest)
            assertTrue { inviteCandidates.usersList.isEmpty() }

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
            coVerify(exactly = 1) { projectMemberRepoMock.getProjectMembers(testProjectId) }
            coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery("john", excludedUsers) }
        }
}
