package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember

class ProjectMemberIntegrationTest : IntegrationTest() {
    @Nested
    inner class InviteAndAccept {
        @Test
        fun `When a user accepts a project invitation, then they appear as a project member`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            val members = projectMemberService.getProjectMembers(project.id)
            assertTrue(members.any { it.user.id == otherUser.id })
        }

        @Test
        fun `When a user who is already a member is invited again, then the invitation is silently ignored`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

                inviteUserToProject(project, otherUser, acceptInvitation = true)

                // Second invite for an already-accepted member: service short-circuits without sending email
                invitationService.inviteUserToProject(
                    GrpcProjectMember.Invite.newBuilder()
                        .setProjectId(project.id.toString())
                        .setUserEmail(otherUser.email)
                        .build(),
                )

                val members = projectMemberService.getProjectMembers(project.id)
                assertEquals(2, members.size) // creator + other user, no duplicate
            }
    }

    @Nested
    inner class RemoveMember {
        @Test
        fun `When an admin removes a project member, then the user no longer appears in the members list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            projectMemberService.removeProjectMember(project.id, otherUser.email)

            val members = projectMemberService.getProjectMembers(project.id)
            assertFalse(members.any { it.user.id == otherUser.id })
        }

        @Test
        fun `When a member removes themselves from a project, then they no longer appear in the members list`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

                inviteUserToProject(project, otherUser, acceptInvitation = true)

                actAsUser(otherUser.id) {
                    projectMemberService.removeProjectMember(project.id, otherUser.email)
                }

                val members = projectMemberService.getProjectMembers(project.id)
                assertFalse(members.any { it.user.id == otherUser.id })
            }
    }

    @Nested
    inner class UpdateMemberRole {
        @Test
        fun `When an admin promotes a member to project admin, then their role is updated`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            projectMemberService.updateProjectMemberRole(
                GrpcProjectMember.Update.newBuilder()
                    .setProjectId(project.id.toString())
                    .setUserId(otherUser.id.toString())
                    .setNewRole(MemberRole.ADMIN.toGrpc())
                    .build(),
            )

            val members = projectMemberService.getProjectMembers(project.id)
            val updatedMember = members.first { it.user.id == otherUser.id }
            assertEquals(MemberRole.ADMIN, updatedMember.projectMember.role)
        }

        @Test
        fun `When an admin demotes a project admin to member, then their role is updated`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            // Promote first
            projectMemberService.updateProjectMemberRole(
                GrpcProjectMember.Update.newBuilder()
                    .setProjectId(project.id.toString())
                    .setUserId(otherUser.id.toString())
                    .setNewRole(MemberRole.ADMIN.toGrpc())
                    .build(),
            )

            // Then demote — still valid because testUser (creator) remains an admin
            projectMemberService.updateProjectMemberRole(
                GrpcProjectMember.Update.newBuilder()
                    .setProjectId(project.id.toString())
                    .setUserId(otherUser.id.toString())
                    .setNewRole(MemberRole.DEFAULT.toGrpc())
                    .build(),
            )

            val members = projectMemberService.getProjectMembers(project.id)
            val demotedMember = members.first { it.user.id == otherUser.id }
            assertEquals(MemberRole.DEFAULT, demotedMember.projectMember.role)
        }
    }
}
