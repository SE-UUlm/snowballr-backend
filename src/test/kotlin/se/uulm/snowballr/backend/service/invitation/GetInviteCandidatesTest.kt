package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.User
import snowballr.ProjectOuterClass.Project.InviteCandidatesRequest
import java.util.UUID
import kotlin.reflect.KFunction

class GetInviteCandidatesTest : InvitationServiceTest() {
    private val requestingUserEmail = "test.user@example.com"
    private val searchQuery = "john"
    private val projectId = UUID.randomUUID()
    private val validGetInviteCandidatesRequestBuilder = InviteCandidatesRequest.newBuilder()
        .setQuery(searchQuery)
        .setProjectId(projectId.toString())

    private fun mockGetInviteCandidates(
        projectMembers: List<User> = emptyList(),
        invitees: List<User> = emptyList(),
        stopBefore: KFunction<*>? = null,
    ) {
        val currentUser = DataBuilder.createExampleUser(email = requestingUserEmail)
        val anotherUser = DataBuilder.createExampleUser(email = "another.user@example.com")
        val users = listOf(currentUser, anotherUser) + projectMembers + invitees
        val projectMembersWithUsers = projectMembers.map {
            ProjectMemberWithUser(DataBuilder.createExampleProjectMember(projectId, it.id), it)
        }
        val inviteeToken = invitees.map {
            DataBuilder.createExampleInvitationToken(email = it.email, projectId = projectId)
        }

        val excludedUsers = setOf(currentUser.email) + projectMembers.map { it.email } + invitees.map { it.email }

        mockCurrentUser(currentUser)
        if (stopBefore == projectMemberRepoMock::getProjectMembersWithUsers) {
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(projectId) } returns projectMembersWithUsers
        coEvery { invitationTokenRepoMock.getActiveInvitationTokensForProject(projectId) } returns inviteeToken
        coEvery {
            userRepoMock.getUsersMatchingSearchQuery(searchQuery, excludedUsers)
        } returns users.filterNot { it.email in excludedUsers }
    }

    @Test
    fun `When the search query is too short, then an empty list is returned`() = runTest {
        mockGetInviteCandidates(stopBefore = projectMemberRepoMock::getProjectMembersWithUsers)

        val shortGetInviteCandidatesRequest = validGetInviteCandidatesRequestBuilder.setQuery("jo")

        val candidates = assertDoesNotThrow { service.getInviteCandidates(shortGetInviteCandidatesRequest.build()) }
        assertThat(candidates.usersList).isEmpty()
    }

    @Test
    fun `When parsing the project id fails, then only a warning is logged`() = runTest {
        mockGetInviteCandidates(stopBefore = projectMemberRepoMock::getProjectMembersWithUsers)
        coEvery {
            userRepoMock.getUsersMatchingSearchQuery(searchQuery, any())
        } returns emptyList()

        val requestWithInvalidProjectId = validGetInviteCandidatesRequestBuilder.setProjectId("invalid-uuid")

        assertDoesNotThrow { service.getInviteCandidates(requestWithInvalidProjectId.build()) }
        coVerify(exactly = 0) { projectMemberRepoMock.getProjectMembersWithUsers(any()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.getActiveInvitationTokensForProject(any()) }
    }

    @Test
    fun `When no project members exist, then no users except for the current user are excluded`() = runTest {
        mockGetInviteCandidates()

        assertDoesNotThrow { service.getInviteCandidates(validGetInviteCandidatesRequestBuilder.build()) }
        coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(requestingUserEmail)) }
    }

    @Test
    fun `When retrieving the invite candidates is successful, then these are returned except for the current user`() =
        runTest {
            mockGetInviteCandidates()

            val inviteCandidates = service.getInviteCandidates(validGetInviteCandidatesRequestBuilder.build())
            assertThat(inviteCandidates.usersList).hasSize(1)
            assertNull(inviteCandidates.usersList.find { it.email == requestingUserEmail })
        }

    @Test
    fun `When retrieving the project members is successful, then these members are not returned as invite candidates`() =
        runTest {
            val projectMember = DataBuilder.createExampleUser(email = "project.member@example.com")
            mockGetInviteCandidates(projectMembers = listOf(projectMember))

            val inviteCandidates = service.getInviteCandidates(validGetInviteCandidatesRequestBuilder.build())
            assertNull(inviteCandidates.usersList.find { it.email == projectMember.email })
        }

    @Test
    fun `When retrieving the invitees is successful, then these invitees are not returned as invite candidates`() =
        runTest {
            val invitee = DataBuilder.createExampleUser(email = "invited.user@example.com")
            mockGetInviteCandidates(invitees = listOf(invitee))

            val inviteCandidates = service.getInviteCandidates(validGetInviteCandidatesRequestBuilder.build())
            assertNull(inviteCandidates.usersList.find { it.email == invitee.email })
        }
}
