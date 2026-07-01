package se.uulm.snowballr.backend.integration.access

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

class AccessControlIntegrationTest : IntegrationTest() {
    private suspend fun setupProjectWithMember(): Pair<Project, User> {
        val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
        val member = addUser(DataBuilder.createExampleUser(email = "member@example.com"))
        inviteUserToProject(project, member, acceptInvitation = true)
        return project to member
    }

    @Nested
    inner class ProjectAccess {
        @Test
        fun `When a non-member tries to read a project, then access is denied`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Private Project"))
            val outsider = addUser(DataBuilder.createExampleUser(email = "outsider@example.com"))

            actAsUser(outsider.id) {
                assertThrows<UnauthorizedException> { projectService.getProjectById(project.id) }
            }
        }

        @Test
        fun `When a non-admin member tries to update a project, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()

            val request = UpdateProjectRequest.fromProject(project)

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { projectService.updateProject(request, setOf("project.name")) }
            }
        }

        @Test
        fun `When a non-admin member tries to delete a project, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { projectService.softDeleteProject(project.id) }
            }
        }
    }

    @Nested
    inner class CriterionAccess {
        @Test
        fun `When a non-admin member tries to create a criterion, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()

            val request = CreateCriterionRequest(
                tag = "BC",
                name = "Blocked Criterion",
                description = "Should not be created",
                category = CriterionCategory.INCLUSION,
                projectId = project.id,
            )

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { criterionService.createCriterion(request) }
            }
        }

        @Test
        fun `When a non-admin member tries to update a criterion, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()

            val criterion = criterionService.createCriterion(
                CreateCriterionRequest(
                    tag = "AC",
                    name = "Admin Criterion",
                    description = "Created by admin",
                    category = CriterionCategory.INCLUSION,
                    projectId = project.id,
                ),
            )

            val request = UpdateCriterionRequest(
                criterion.id,
                criterion.tag,
                "Hijacked",
                criterion.description,
                criterion.category,
            )

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> {
                    criterionService.updateCriterion(request, listOf("criterion.name"))
                }
            }
        }
    }

    @Nested
    inner class ProjectPaperAccess {
        @Test
        fun `When a non-admin member tries to add a paper to a project, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()
            val paper = createPaper()

            val request = GrpcProjectPaper.Add.newBuilder()
                .setProjectId(project.id.toString())
                .setPaperId(paper.id.toString())
                .setStage(0)
                .build()

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { projectPaperService.addPaperToProject(request) }
            }
        }
    }

    @Nested
    inner class InvitationAccess {
        @Test
        fun `When a non-admin member tries to invite a user to a project, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()

            val request = GrpcProjectMember.Invite.newBuilder()
                .setProjectId(project.id.toString())
                .setUserEmail("uninvited@example.com")
                .build()

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { invitationService.inviteUserToProject(request) }
            }
        }
    }

    @Nested
    inner class MemberManagementAccess {
        @Test
        fun `When a non-admin member tries to update another member's role, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()
            val secondMember = addUser(DataBuilder.createExampleUser(email = "second.member@example.com"))
            inviteUserToProject(project, secondMember, acceptInvitation = true)

            val request = GrpcProjectMember.Update.newBuilder()
                .setProjectId(project.id.toString())
                .setUserId(secondMember.id.toString())
                .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)
                .build()

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { projectMemberService.updateProjectMemberRole(request) }
            }
        }

        @Test
        fun `When a non-admin member tries to remove another member, then access is denied`() = runTest {
            val (project, member) = setupProjectWithMember()
            val secondMember = addUser(DataBuilder.createExampleUser(email = "second.member@example.com"))
            inviteUserToProject(project, secondMember, acceptInvitation = true)

            val request = GrpcProjectMember.Remove.newBuilder()
                .setProjectId(project.id.toString())
                .setUserEmail(secondMember.email)
                .build()

            actAsUser(member.id) {
                assertThrows<UnauthorizedException> { projectMemberService.removeProjectMember(request) }
            }
        }
    }
}
