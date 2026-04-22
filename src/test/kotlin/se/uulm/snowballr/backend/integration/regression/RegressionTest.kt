package se.uulm.snowballr.backend.integration.regression

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.ProjectOuterClass.Project
import snowballr.UserOuterClass.UserStatus
import kotlin.test.assertEquals

class RegressionTest : IntegrationTest() {
    @Test
    fun `When the invitation of an existing user is removed from a project, then the invitation token is removed`() =
        runTest {
            // Create a project
            val createProjectRequest = Project.Create.newBuilder()
                .setName("Test Project")
                .build()
            val project = mainService.createProject(createProjectRequest)

            // Register another user to invite
            val otherUser = DataBuilder.createExampleUser()
            addUser(otherUser)

            // Invite the other user to the project
            inviteUserToProject(project, otherUser)

            val id = parseUUID(project.id, EntityType.PROJECT)
            var pendingInvitations = mainService.getPendingInvitationsForProject(id).usersList
            assertEquals(1, pendingInvitations.size)
            assertEquals(otherUser.email, pendingInvitations[0].email)
            assertEquals(otherUser.firstName, pendingInvitations[0].firstName)
            assertEquals(otherUser.lastName, pendingInvitations[0].lastName)
            assertEquals(UserStatus.USER_STATUS_ACTIVE, pendingInvitations[0].status)

            // Remove the other user's invitation from the project
            val removeInvitationRequest = Project.Member.Remove.newBuilder()
                .setProjectId(project.id)
                .setUserEmail(otherUser.email)
                .build()
            mainService.removeProjectMember(removeInvitationRequest)

            pendingInvitations = mainService.getPendingInvitationsForProject(id).usersList
            assertEquals(0, pendingInvitations.size)
        }
}
