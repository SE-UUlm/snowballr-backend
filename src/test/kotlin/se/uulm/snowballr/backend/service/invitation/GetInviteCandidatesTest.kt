package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.user.User
import java.util.UUID
import kotlin.reflect.KFunction

class GetInviteCandidatesTest : InvitationServiceTest() {
    private val requestingUserEmail = "test.user@example.com"
    private val searchQuery = "john"
    private val projectId = UUID.randomUUID()

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

        val candidates = service.getInviteCandidates(projectId, "jo")
        assertThat(candidates).isEmpty()
    }

    @Test
    fun `When no project members exist, then no users except for the current user are excluded`() = runTest {
        mockGetInviteCandidates()

        service.getInviteCandidates(projectId, searchQuery)
        coVerify(exactly = 1) { userRepoMock.getUsersMatchingSearchQuery(searchQuery, setOf(requestingUserEmail)) }
    }

    @Test
    fun `When retrieving the invite candidates is successful, then these are returned except for the current user`() =
        runTest {
            mockGetInviteCandidates()

            val inviteCandidates = service.getInviteCandidates(projectId, searchQuery)
            assertThat(inviteCandidates).hasSize(1)
            assertNull(inviteCandidates.find { it.email == requestingUserEmail })
        }

    @Test
    fun `When retrieving the project members is successful, then these members are not returned as invite candidates`() =
        runTest {
            val projectMember = DataBuilder.createExampleUser(email = "project.member@example.com")
            mockGetInviteCandidates(projectMembers = listOf(projectMember))

            val inviteCandidates = service.getInviteCandidates(projectId, searchQuery)
            assertNull(inviteCandidates.find { it.email == projectMember.email })
        }

    @Test
    fun `When retrieving the invitees is successful, then these invitees are not returned as invite candidates`() =
        runTest {
            val invitee = DataBuilder.createExampleUser(email = "invited.user@example.com")
            mockGetInviteCandidates(invitees = listOf(invitee))

            val inviteCandidates = service.getInviteCandidates(projectId, searchQuery)
            assertNull(inviteCandidates.find { it.email == invitee.email })
        }
}
