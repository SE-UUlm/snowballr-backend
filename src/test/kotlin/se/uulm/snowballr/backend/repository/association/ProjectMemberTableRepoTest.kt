package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.repository.RepositoryHelper
import se.uulm.snowballr.backend.repository.RepositoryHelper.createAndAssignUserToProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.repository.UserTableRepo
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.ProjectOuterClass.MemberRole
import java.sql.SQLException
import java.util.UUID

class ProjectMemberTableRepoTest : RepositoryTest(arrayOf(ProjectTable, ProjectMemberTable), true) {
    private val repo = ProjectMemberTableRepo(db)
    private val userRepo = UserTableRepo(db)

    private suspend fun setupProject(
        numberOfTestMembers: Int = 0,
        addTestUser: Boolean = true,
    ): Pair<UUID, List<ProjectMember>> {
        val projectId = insertProjectAndGetId(createdBy = testUserId)
        val members = mutableListOf<ProjectMember>()

        if (addTestUser) {
            repo.addUserToProject(testUserId, projectId).also { members.add(it) }
        }

        for (i in 1..numberOfTestMembers) {
            createAndAssignUserToProject("test.user$i@example.com", projectId).also { members.add(it) }
        }

        return projectId to members
    }

    @Nested
    inner class GetProjectMemberByComposedId {
        @Test
        fun `When a project member is found, then a successful result with the correct project member is returned`() =
            runTest {
                val (projectId, members) = setupProject()
                val result = repo.getProjectMemberByComposedId(projectId, members[0].userId)

                val projectMember = assertResultSuccess(result)
                assertEquals(projectId, projectMember.projectId)
                assertEquals(testUserId, projectMember.userId)
                assertEquals(members[0].role, projectMember.role)
            }

        @Test
        fun `When a project member is not found, then a failed result with a NotFoundException is returned`() =
            runTest {
                val result = repo.getProjectMemberByComposedId(UUID.randomUUID(), UUID.randomUUID())

                assertResultFailure<NotFoundException>(result)
            }
    }

    @Nested
    inner class AddUserToProject {
        @Test
        fun `When a user is added to a project, then they can be retrieved as project member`() = runTest {
            val (projectId, members) = setupProject()
            val member = members.first()

            assertEquals(testUserId, member.userId)
            assertEquals(projectId, member.projectId)
            assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, member.role)

            val actualMembers = repo.getProjectMembers(projectId)
            assertThat(actualMembers).hasSize(1)
            assertThat(actualMembers).containsExactly(member)
        }

        @Test
        fun `When a user is added to a project twice, then only one member is created`() = runTest {
            val (projectId, members) = setupProject()
            val member = members.first()
            val member1 = repo.addUserToProject(testUserId, projectId)

            assertEquals(member, member1)

            val actualMembers = repo.getProjectMembers(projectId)
            assertThat(actualMembers).hasSize(1)
            assertThat(actualMembers).containsExactly(member)
        }

        @Test
        fun `When a user is added to a nonexistent project, then an SQLException is thrown`() = runTest {
            assertThrows<SQLException> {
                repo.addUserToProject(testUserId, UUID.randomUUID())
            }
        }

        @Test
        fun `When a nonexistent user is added to a project, then an SQLException is thrown`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            assertThrows<SQLException> {
                repo.addUserToProject(UUID.randomUUID(), projectId)
            }
        }
    }

    @Nested
    inner class GetProjectMembers {
        @Test
        fun `When no members are in a project, then the list is empty`() = runTest {
            val (projectId, _) = setupProject(0, false)

            val members = repo.getProjectMembers(projectId)

            assertThat(members).isEmpty()
        }

        @Test
        fun `When one member is in a project, then they are part of the list`() = runTest {
            val (projectId, _) = setupProject()

            val actualMembers = repo.getProjectMembers(projectId)

            assertThat(actualMembers).hasSize(1)
            val member = actualMembers.first()
            assertEquals(testUserId, member.userId)
        }

        @Test
        fun `When several members are in a project, then they are part of the list`() = runTest {
            val (projectId, members) = setupProject(3)

            val actualMembers = repo.getProjectMembers(projectId)

            assertThat(actualMembers).hasSize(4)
            assertEquals(testUserId, actualMembers[0].userId)
            assertEquals(members[0], actualMembers[0])
            assertEquals(members[1], actualMembers[1])
            assertEquals(members[2], actualMembers[2])
            assertEquals(members[3], actualMembers[3])
        }
    }

    @Nested
    inner class GetMembersInSameProjectsAsUser {
        @Test
        fun `When the user is in no projects, then the list of members is empty`() = runTest {
            val result = repo.getMembersInSameProjectsAsUser(testUserId)

            assertThat(result).isEmpty()
        }

        @Test
        fun `When the user is in a project, then they are not part of the list of members`() = runTest {
            setupProject()

            val result = repo.getMembersInSameProjectsAsUser(testUserId)

            assertThat(result).isEmpty()
        }

        @Test
        fun `When the user is in projects with other users, then all members are part of the list`() = runTest {
            val project1Id = setupProject().first
            val project2Id = setupProject().first
            val project3Id = setupProject().first

            val member1 = createAndAssignUserToProject("test.user1@example.com", project1Id)
            val member2 = createAndAssignUserToProject("test.user2@example.com", project2Id)
            val member3 = createAndAssignUserToProject("test.user3@example.com", project3Id)

            val result = repo.getMembersInSameProjectsAsUser(testUserId)

            assertThat(result).hasSize(3)
            assertThat(result).containsExactlyInAnyOrder(member1, member2, member3)
        }
    }

    @Nested
    inner class GetAllProjectAdmins {
        @Test
        fun `When all project members are project admins, then the correct list of project admins is returned`() =
            runTest {
                val (projectId, members) = setupProject(1)
                val firstMember = repo.getProjectMemberByComposedId(projectId, testUserId).getOrThrow()
                val secondMember = repo.getProjectMemberByComposedId(projectId, members[1].userId).getOrThrow()
                assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, firstMember.role)
                assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, secondMember.role)

                repo.updateProjectMemberRole(projectId, firstMember.userId, MemberRole.MEMBER_ROLE_ADMIN)
                repo.updateProjectMemberRole(projectId, secondMember.userId, MemberRole.MEMBER_ROLE_ADMIN)

                val projectAdmins = repo.getAllProjectAdmins(projectId)

                projectAdmins.forEachIndexed { index, admin ->
                    assertEquals(projectId, admin.projectId)
                    assertEquals(members[index].userId, admin.userId)
                    assertEquals(MemberRole.MEMBER_ROLE_ADMIN, admin.role)
                }
            }

        @Test
        fun `When not all project members are project admins, then the correct list of project admins is returned`() =
            runTest {
                val (projectId, members) = setupProject(1)
                val firstMember = repo.getProjectMemberByComposedId(projectId, testUserId).getOrThrow()
                val secondMember = repo.getProjectMemberByComposedId(projectId, members[1].userId).getOrThrow()
                assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, firstMember.role)
                assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, secondMember.role)

                repo.updateProjectMemberRole(projectId, firstMember.userId, MemberRole.MEMBER_ROLE_ADMIN)

                val projectAdmins = repo.getAllProjectAdmins(projectId)

                assertThat(projectAdmins).hasSize(1)
                assertThat(projectAdmins).anyMatch { it.userId == firstMember.userId }
                assertThat(projectAdmins).noneMatch { it.userId == secondMember.userId }
            }
    }

    @Nested
    inner class PromoteProjectMemberToAdmin {
        @Test
        fun `When a project member is promoted to admin, then the role of the project member is correctly updated`() =
            runTest {
                val (projectId, _) = setupProject()
                val normalMember = repo.getProjectMemberByComposedId(projectId, testUserId).getOrThrow()
                assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, normalMember.role)

                val promotedMember = repo.updateProjectMemberRole(projectId, testUserId, MemberRole.MEMBER_ROLE_ADMIN)

                assertEquals(projectId, promotedMember.projectId)
                assertEquals(testUserId, promotedMember.userId)
                assertEquals(MemberRole.MEMBER_ROLE_ADMIN, promotedMember.role)
            }

        @Test
        fun `When a project admin is promoted, then the role of the project admin does not change`() = runTest {
            val (projectId, _) = setupProject()
            var promotedMember = repo.updateProjectMemberRole(projectId, testUserId, MemberRole.MEMBER_ROLE_ADMIN)
            assertEquals(MemberRole.MEMBER_ROLE_ADMIN, promotedMember.role)

            promotedMember = repo.updateProjectMemberRole(projectId, testUserId, MemberRole.MEMBER_ROLE_ADMIN)

            assertEquals(projectId, promotedMember.projectId)
            assertEquals(testUserId, promotedMember.userId)
            assertEquals(MemberRole.MEMBER_ROLE_ADMIN, promotedMember.role)
        }
    }

    @Nested
    inner class UpdateProjectMemberRole {
        @Test
        fun `When a project member is updated, then the role of the project member is correctly updated`() = runTest {
            val (projectId, _) = setupProject()
            val normalMember = repo.getProjectMemberByComposedId(projectId, testUserId).getOrThrow()
            assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, normalMember.role)

            val updatedMember = repo.updateProjectMemberRole(projectId, testUserId, MemberRole.MEMBER_ROLE_ADMIN)

            assertEquals(projectId, updatedMember.projectId)
            assertEquals(testUserId, updatedMember.userId)
            assertEquals(MemberRole.MEMBER_ROLE_ADMIN, updatedMember.role)
        }
    }

    @Nested
    inner class GetProjectMembersWithUsers {
        @Test
        fun `When a project member and the corresponding user exists, then the project member with user is correctly returned`() =
            runTest {
                val (projectId, _) = setupProject()
                val normalMember = repo.getProjectMemberByComposedId(projectId, testUserId).getOrThrow()
                val user = userRepo.getUserById(testUserId).getOrThrow()
                val projectMembersWithUsers = repo.getProjectMembersWithUsers(projectId)

                assertThat(projectMembersWithUsers).hasSize(1)
                assertThat(projectMembersWithUsers).anyMatch { it.projectMember == normalMember }
                assertThat(projectMembersWithUsers).anyMatch { it.user == user }
            }

        @Test
        fun `When not all users are project members, then only the correct project members with users are returned`() =
            runTest {
                val (projectId, _) = setupProject()
                val user = userRepo.getUserById(testUserId).getOrThrow()
                val nonProjectMemberUserId = RepositoryHelper.insertUserAndGetId("test-user@example.com")
                val nonProjectMemberUser = userRepo.getUserById(nonProjectMemberUserId).getOrThrow()
                val normalMember = repo.getProjectMemberByComposedId(projectId, testUserId).getOrThrow()
                val projectMembersWithUsers = repo.getProjectMembersWithUsers(projectId)

                assertThat(projectMembersWithUsers).hasSize(1)
                assertThat(projectMembersWithUsers).anyMatch { it.projectMember == normalMember }
                assertThat(projectMembersWithUsers).anyMatch { it.user == user }
                assertThat(projectMembersWithUsers).noneMatch { it.projectMember.userId == nonProjectMemberUser.id }
            }
    }

    @Nested
    inner class RemoveProjectMember {
        @Test
        fun `When a project member is found, then the correct project member is removed`() = runTest {
            val (projectId, members) = setupProject()
            repo.removeProjectMember(projectId, members[0].userId)

            assertTrue(repo.getProjectMemberByComposedId(projectId, members[0].userId).isFailure)
        }

        @Test
        fun `When a project member is not found, nothing happens`() = runTest {
            val (projectId, members) = setupProject()

            assertDoesNotThrow { repo.removeProjectMember(projectId, UUID.randomUUID()) }
            assertDoesNotThrow { repo.removeProjectMember(UUID.randomUUID(), members[0].userId) }
            assertTrue(repo.getProjectMemberByComposedId(projectId, members[0].userId).isSuccess)
        }
    }
}
