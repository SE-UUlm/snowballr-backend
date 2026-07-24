package se.uulm.snowballr.backend.integration.regression

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse

class RegressionTest : IntegrationTest() {
    @Test
    fun `When the invitation of an existing user is removed from a project, then the invitation token is removed`() =
        runTest {
            // Create a project
            val createProjectRequest = CreateProjectRequest(name = "Test Project")
            val project = projectService.createProject(createProjectRequest)

            // Register another user to invite
            val otherUser = addUser(DataBuilder.createExampleUser())

            // Invite the other user to the project
            inviteUserToProject(project, otherUser)

            var pendingInvitations = invitationService.getPendingInvitationsForProject(project.id)
            assertEquals(1, pendingInvitations.size)
            assertEquals(otherUser.email, pendingInvitations[0].email)
            assertEquals(otherUser.firstName, pendingInvitations[0].firstName)
            assertEquals(otherUser.lastName, pendingInvitations[0].lastName)
            assertEquals(UserStatus.ACTIVE, pendingInvitations[0].status)

            // Remove the other user's invitation from the project
            projectMemberService.removeProjectMember(project.id, otherUser.email)

            pendingInvitations = invitationService.getPendingInvitationsForProject(project.id)
            assertEquals(0, pendingInvitations.size)
        }

    @Test
    fun `When multiple papers are added to the project concurrently, then they all have a different local ID`() =
        runTest {
            val numberOfPapers = 10
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val papers = mutableSetOf<PaperResponse>()
            for (i in 1..numberOfPapers) {
                val request = CreatePaperRequest.fromPaper(
                    DataBuilder.createExamplePaper(
                        title = "Paper $i",
                        externalIds = listOf(ExternalId(ExternalIdType.URL, "External ID $i")),
                    ),
                )
                papers += paperService.createPaper(request)
            }

            val projectPapers = papers.map {
                async {
                    projectPaperService.addPaperToProject(project.id, it.id, 0)
                }
            }.awaitAll()

            val localIds = projectPapers.map { it.localPaperId }.distinct()
            assertEquals(numberOfPapers, localIds.size)

            val projectPaper = projectPapers.first()
            assertDoesNotThrow {
                // If several papers have the same local ID this would throw
                projectPaperService.getProjectPaperByRelativeId(project.id, projectPaper.localPaperId)
            }
        }
}
