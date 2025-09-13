package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.Project.InviteCandidatesRequest
import java.util.UUID
import kotlin.test.assertTrue

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
class GetInviteCandidatesTest : MainServiceTest() {
    private val testProjectId = UUID.randomUUID()
    private val validInviteCandidatesRequest = InviteCandidatesRequest.newBuilder()
        .setQuery("john")
        .setProjectId(testProjectId.toString())
        .build()

    @Test
    fun `When the search query is too short, then an empty list is returned`() = runTest {
        val shortSearchQuery = InviteCandidatesRequest.newBuilder().setQuery("j").build()

        assertDoesNotThrow { mainService.getInviteCandidates(shortSearchQuery) }
    }

    @Test
    fun `When parsing the project id fails, then only a warning is logged`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestWithInvalidProjectId = InviteCandidatesRequest
            .newBuilder()
            .setQuery("john")
            .setProjectId("invalid-uuid")
            .build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(requestWithInvalidProjectId) }
    }

    @Test
    fun `When no the project members exist, then no users except for the current user are excluded`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedProjectId = UUID.randomUUID()
        val requestWithNotExistingProject = InviteCandidatesRequest
            .newBuilder()
            .setQuery("john")
            .setProjectId(requestedProjectId.toString())
            .build()

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(requestedProjectId) } returns emptyList()
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(requestWithNotExistingProject) }
    }

    @Test
    fun `When retrieving the users matching the search query fails, then an empty list is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(testProjectId) } returns emptyList()
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(validInviteCandidatesRequest) }
    }

    @Test
    fun `When retrieving the invite candidates is successful, then these are returned except for the current user`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = "current.user@example.com")
            val users = listOf(currentUser, DataBuilder.createExampleUser(email = "another.user@example.com"))
            val excludedUsers = setOf(currentUser.id)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(testProjectId) } returns emptyList()
            coEvery {
                userRepoMock.getUsersMatchingSearchQuery(any(), setOf(currentUser.id))
            } returns users.filterNot { it.id in excludedUsers }

            val inviteCandidates = mainService.getInviteCandidates(validInviteCandidatesRequest)
            assertTrue { inviteCandidates.usersList.size == 1 }
            assertTrue { inviteCandidates.usersList.find { it.id == currentUser.id.toString() } == null }
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
                projectMemberRepoMock.getProjectMembers(testProjectId)
            } returns listOf(DataBuilder.createExampleProjectMember(userId = projectMember.id))
            coEvery {
                userRepoMock.getUsersMatchingSearchQuery(any(), setOf(currentUser.id, projectMember.id))
            } returns users.filterNot { it.id in excludedUsers }

            val inviteCandidates = mainService.getInviteCandidates(validInviteCandidatesRequest)
            assertTrue { inviteCandidates.usersList.isEmpty() }
        }
}
