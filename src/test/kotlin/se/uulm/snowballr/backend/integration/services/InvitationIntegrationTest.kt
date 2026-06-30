package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.ProjectOuterClass.Project
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
                val projectId = parseUUID(project.id, EntityType.PROJECT)
                val inviteeEmail = "pending.user@example.com"

                inviteEmailToProject(project, inviteeEmail)

                val pending = invitationService.getPendingInvitationsForProject(projectId)
                assertTrue(pending.usersList.any { it.email == inviteeEmail })
            }

        @Test
        fun `When an invited user accepts their invitation, then they no longer appear in the pending list`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val projectId = parseUUID(project.id, EntityType.PROJECT)
                val otherUser = addUser(DataBuilder.createExampleUser(email = "accepting.user@example.com"))

                inviteUserToProject(project, otherUser, acceptInvitation = true)

                val pending = invitationService.getPendingInvitationsForProject(projectId)
                assertFalse(pending.usersList.any { it.email == otherUser.email })
            }

        @Test
        fun `When no users have been invited, then the pending invitations list is empty`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val projectId = parseUUID(project.id, EntityType.PROJECT)

            val pending = invitationService.getPendingInvitationsForProject(projectId)
            assertTrue(pending.usersList.isEmpty())
        }

        @Test
        fun `When a user is invited a second time while already pending, then the pending list has only one entry`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val projectId = parseUUID(project.id, EntityType.PROJECT)
                val inviteeEmail = "double.invite@example.com"

                inviteEmailToProject(project, inviteeEmail)

                // Second invite for already-pending user: service short-circuits without sending another email
                invitationService.inviteUserToProject(
                    Project.Member.Invite.newBuilder()
                        .setProjectId(project.id)
                        .setUserEmail(inviteeEmail)
                        .build(),
                )

                val pending = invitationService.getPendingInvitationsForProject(projectId)
                assertEquals(1, pending.usersList.count { it.email == inviteeEmail })
            }
    }

    @Nested
    inner class GetInviteCandidates {
        @Test
        fun `When the search query is too short, then an empty candidate list is returned`() = runTest {
            addUser(DataBuilder.createExampleUser(email = "searchable.user@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val candidates = invitationService.getInviteCandidates(
                Project.InviteCandidatesRequest.newBuilder()
                    .setQuery("ab")
                    .setProjectId(project.id)
                    .build(),
            )

            assertTrue(candidates.usersList.isEmpty())
        }

        @Test
        fun `When the search query matches a registered user, then that user is returned as a candidate`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "findable.candidate@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val candidates = invitationService.getInviteCandidates(
                Project.InviteCandidatesRequest.newBuilder()
                    .setQuery(otherUser.email.take(6))
                    .setProjectId(project.id)
                    .build(),
            )

            assertTrue(candidates.usersList.any { it.id == otherUser.id.toString() })
        }

        @Test
        fun `When a user is already a project member, then they are excluded from invite candidates`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "already.member@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            val candidates = invitationService.getInviteCandidates(
                Project.InviteCandidatesRequest.newBuilder()
                    .setQuery(otherUser.email.take(6))
                    .setProjectId(project.id)
                    .build(),
            )

            assertFalse(candidates.usersList.any { it.id == otherUser.id.toString() })
        }

        @Test
        fun `When a user has a pending invitation, then they are excluded from invite candidates`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "pending.candidate@example.com"))
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            inviteUserToProject(project, otherUser)

            val candidates = invitationService.getInviteCandidates(
                Project.InviteCandidatesRequest.newBuilder()
                    .setQuery(otherUser.email.take(7))
                    .setProjectId(project.id)
                    .build(),
            )

            assertFalse(candidates.usersList.any { it.id == otherUser.id.toString() })
        }
    }
}
