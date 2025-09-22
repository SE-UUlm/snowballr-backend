package se.uulm.snowballr.backend.service.invitations

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.Project.InviteCandidatesRequest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetInviteCandidatesTest : MainServiceTest() {
    private val searchQuery = "john"
    private val projectId = UUID.randomUUID()
    private val validInviteCandidatesRequest = InviteCandidatesRequest.newBuilder()
        .setQuery(searchQuery)
        .setProjectId(projectId.toString())
        .build()

    @Test
    fun `When the search query is too short, then an empty list is returned`() = runTest {
        val shortSearchQuery = InviteCandidatesRequest.newBuilder().setQuery("j").build()

        mockCurrentUser(DataBuilder.createExampleUser())

        val candidates = assertDoesNotThrow { mainService.getInviteCandidates(shortSearchQuery) }
        assertTrue { candidates.usersList.isEmpty() }
    }

    @Test
    fun `When parsing the project id fails, then only a warning is logged`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestWithInvalidProjectId = InviteCandidatesRequest
            .newBuilder()
            .setQuery(searchQuery)
            .setProjectId("invalid-uuid")
            .build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(currentUser.id)) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(requestWithInvalidProjectId) }
    }

    @Test
    fun `When no project members exist, then no users except for the current user are excluded`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()
        coEvery { userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(currentUser.id)) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(validInviteCandidatesRequest) }
    }

    @Test
    fun `When retrieving the users matching the search query fails, then an empty list is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()
        coEvery { userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(currentUser.id)) } returns emptyList()

        val candidates = assertDoesNotThrow { mainService.getInviteCandidates(validInviteCandidatesRequest) }
        assertTrue { candidates.usersList.isEmpty() }
    }

    @Test
    fun `When retrieving the invite candidates is successful, then these are returned except for the current user`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = "current.user@example.com")
            val anotherUser = DataBuilder.createExampleUser(email = "another.user@example.com")
            val users = listOf(currentUser, anotherUser)
            val excludedUsers = setOf(currentUser.id)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()
            coEvery {
                userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(currentUser.id))
            } returns users.filterNot { it.id in excludedUsers }

            val inviteCandidates = mainService.getInviteCandidates(validInviteCandidatesRequest)
            assertEquals(1, inviteCandidates.usersList.size)
            assertNull(inviteCandidates.usersList.find { user -> user.id == currentUser.id.toString() })
        }

    @Test
    fun `When retrieving the project members is successful, then these members are not returned as invite candidates`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = "current.user@example.com")
            val projectMember = DataBuilder.createExampleUser(email = "project.member@example.com")
            val users = listOf(currentUser, projectMember)
            val excludedUsers = setOf(currentUser.id, projectMember.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectMemberRepoMock.getProjectMembers(projectId)
            } returns listOf(DataBuilder.createExampleProjectMember(userId = projectMember.id))
            coEvery {
                userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(currentUser.id, projectMember.id))
            } returns users.filterNot { it.id in excludedUsers }

            val inviteCandidates = mainService.getInviteCandidates(validInviteCandidatesRequest)
            assertTrue { inviteCandidates.usersList.isEmpty() }
        }
}
