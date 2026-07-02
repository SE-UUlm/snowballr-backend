package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvitationIntegrationTest : IntegrationTest() {
    @Nested
    inner class GetPendingInvitations {
        @Test
        fun `When a user is invited but has not accepted, then they appear in the pending invitations list`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val inviteeEmail = "pending.user@example.com"

                inviteEmailToProject(project, inviteeEmail)

                val pending = invitationService.getPendingInvitationsForProject(project.id)
                assertTrue(pending.any { it.email == inviteeEmail })
            }

        @Test
        fun `When an invited user accepts their invitation, then they no longer appear in the pending list`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val otherUser = addUser(DataBuilder.createExampleUser(email = "accepting.user@example.com"))

                inviteUserToProject(project, otherUser, acceptInvitation = true)

                val pending = invitationService.getPendingInvitationsForProject(project.id)
                assertFalse(pending.any { it.email == otherUser.email })
            }

        @Test
        fun `When no users have been invited, then the pending invitations list is empty`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val pending = invitationService.getPendingInvitationsForProject(project.id)
            assertTrue(pending.isEmpty())
        }

        @Test
        fun `When a user is invited a second time while already pending, then the pending list has only one entry`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val inviteeEmail = "double.invite@example.com"

                inviteEmailToProject(project, inviteeEmail)

                // Second invite for already-pending user: service short-circuits without sending another email
                invitationService.inviteUserToProject(project.id, inviteeEmail)

                val pending = invitationService.getPendingInvitationsForProject(project.id)
                assertEquals(1, pending.count { it.email == inviteeEmail })
            }
    }

    @Nested
    inner class GetInviteCandidates {
        @Test
        fun `When the search query is too short, then an empty candidate list is returned`() = runTest {
            addUser(DataBuilder.createExampleUser(email = "searchable.user@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val candidates = invitationService.getInviteCandidates(project.id, "ab")

            assertTrue(candidates.isEmpty())
        }

        @Test
        fun `When the search query matches a registered user, then that user is returned as a candidate`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "findable.candidate@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val candidates = invitationService.getInviteCandidates(project.id, otherUser.email.take(6))

            assertTrue(candidates.any { it.id == otherUser.id })
        }

        @Test
        fun `When a user is already a project member, then they are excluded from invite candidates`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "already.member@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            val candidates = invitationService.getInviteCandidates(project.id, otherUser.email.take(6))

            assertFalse(candidates.any { it.id == otherUser.id })
        }

        @Test
        fun `When a user has a pending invitation, then they are excluded from invite candidates`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "pending.candidate@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            inviteUserToProject(project, otherUser)

            val candidates = invitationService.getInviteCandidates(project.id, otherUser.email.take(7))

            assertFalse(candidates.any { it.id == otherUser.id })
        }
    }
}
