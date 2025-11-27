package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.exception.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.assignUserToProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.createAndAssignUserToProject
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.repository.UserTableRepo
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.ProjectOuterClass.MemberRole
import snowballr.UserOuterClass.UserStatus
import java.sql.SQLException
import java.util.UUID

class ProjectMemberTableRepoTest : RepositoryTest(arrayOf(ProjectTable, ProjectMemberTable), true) {
    private val repo = ProjectMemberTableRepo(db)
    private val userRepo = UserTableRepo(db)

    private suspend fun setupProject(
        numberOfAdditionalMembers: Int = 0,
        addTestUser: Boolean = true,
        addDeletedUser: Boolean = false,
    ): Pair<UUID, List<ProjectMember>> {
        val projectId = insertProjectAndGetId(createdBy = testUserId)
        val members = mutableListOf<ProjectMember>()

        if (addTestUser) {
            members += assignUserToProject(testUserId, projectId)
        }

        if (addDeletedUser) {
            val deletedUser = insertUserAndGetId(
                email = "deleted.user@example.com",
                status = UserStatus.USER_STATUS_DELETED,
            )
            members += assignUserToProject(deletedUser, projectId)
        }

        for (i in 1..numberOfAdditionalMembers) {
            val userEmail = "test.user$i.in.${projectId.toString().substring(0,3)}@example.com"
            members += createAndAssignUserToProject(userEmail, projectId)
        }

        return projectId to members
    }

    /**
     * Checks that the given [expected] and [actual] project members are equal, i.e., that they have the same
     *   - project ID
     *   - user ID
     *   - role
     */
    private fun assertSameMember(expected: ProjectMember, actual: ProjectMember) {
        assertThat(actual.projectId).isEqualTo(expected.projectId)
        assertThat(actual.userId).isEqualTo(expected.userId)
        assertThat(actual.role).isEqualTo(expected.role)
    }

    @Nested
    inner class GetProjectMemberByComposedId {
        @Test
        fun `When a project member is found, then a successful result with the correct project member is returned`() =
            runTest {
                val (projectId, members) = setupProject()
                val result = repo.getProjectMemberByComposedId(projectId, members.first().userId)

                val member = assertResultSuccess(result)
                assertSameMember(members.first(), member)
            }

        @Test
        fun `When a project member is not found, then a failed result with a NotFoundException is returned`() =
            runTest {
                val result = repo.getProjectMemberByComposedId(UUID.randomUUID(), UUID.randomUUID())

                assertResultFailure<NotFoundException>(result)
            }

        @Test
        fun `When a project member is found, but the user is marked as deleted, then a failed result with a NotFoundException is returned`() =
            runTest {
                val (projectId, members) = setupProject(addTestUser = false, addDeletedUser = true)

                val result = repo.getProjectMemberByComposedId(projectId, members.first().userId)

                assertResultFailure<NotFoundException>(result)
            }
    }

    @Nested
    inner class AddUserToProject {
        @Test
        fun `When a user is added to a project, then they can be retrieved as project member`() = runTest {
            val (projectId, _) = setupProject()

            val members = repo.getProjectMembers(projectId)
            assertThat(members).hasSize(1)

            val newUser = insertUserAndGetId("new.user@example.com")
            val newMember = repo.addUserToProject(newUser, projectId)

            val newMembers = repo.getProjectMembers(projectId)
            assertThat(newMembers).hasSize(2)
            assertThat(newMembers).containsExactlyInAnyOrder(members.first(), newMember)
        }

        @Test
        fun `When a user is added to a project twice, then only one member is created`() = runTest {
            val (projectId, members) = setupProject()
            val member = members.first()
            val sameMember = repo.addUserToProject(member.userId, member.projectId)

            assertSameMember(member, sameMember)

            val actualMembers = repo.getProjectMembers(projectId)
            assertThat(actualMembers).hasSize(1)
            assertThat(actualMembers).containsExactly(member)
        }

        @Test
        fun `When a user is added to a nonexistent project, then an SQLException is thrown`() = runTest {
            assertThrows<SQLException> { repo.addUserToProject(testUserId, UUID.randomUUID()) }
        }

        @Test
        fun `When a nonexistent user is added to a project, then an SQLException is thrown`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)

            assertThrows<SQLException> { repo.addUserToProject(UUID.randomUUID(), projectId) }
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
            val (projectId, members) = setupProject()

            val actualMembers = repo.getProjectMembers(projectId)

            assertThat(actualMembers).hasSize(1)
            assertSameMember(members.first(), actualMembers.first())
        }

        @Test
        fun `When several members are in a project, then they are part of the list, except the user is soft-deleted`() =
            runTest {
                val (projectId, members) = setupProject(
                    numberOfAdditionalMembers = 3,
                    addTestUser = true,
                    addDeletedUser = true,
                )

                val actualMembers = repo.getProjectMembers(projectId)

                assertThat(actualMembers).hasSize(4)
                // The deleted user is the second member if the test user and deleted user are assigned to the project
                assertThat(actualMembers).doesNotContain(members[1])
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
        fun `When the user is in a project without other users, then they are not part of the list of members`() =
            runTest {
                val (_, members) = setupProject()
                assertThat(members).hasSize(1)

                val result = repo.getMembersInSameProjectsAsUser(testUserId)

                assertThat(result).isEmpty()
            }

        @Test
        fun `When the user is in projects with other users, then they are part of the list of members`() = runTest {
            val (_, members1) = setupProject(numberOfAdditionalMembers = 1)
            val (_, members2) = setupProject(numberOfAdditionalMembers = 1)

            val result = repo.getMembersInSameProjectsAsUser(testUserId)

            assertThat(result).hasSize(2)
            assertThat(result).containsExactlyInAnyOrder(members1[1], members2[1])
        }

        @Test
        fun `When the user is in a project with another user who is soft-deleted, then they are not part of the list of members`() =
            runTest {
                val (_, members1) = setupProject(addDeletedUser = true)
                val (_, members2) = setupProject(numberOfAdditionalMembers = 1)

                val result = repo.getMembersInSameProjectsAsUser(testUserId)

                assertThat(result).hasSize(1)
                assertThat(result).containsExactlyInAnyOrder(members2[1])
                assertThat(result).doesNotContain(members1[1])
            }
    }

    @Nested
    inner class GetAllProjectAdmins {
        @Test
        fun `When all project members are project admins, then they are all part of the list of project admins`() =
            runTest {
                val (projectId, members) = setupProject(1)

                members.forEach { member ->
                    val actualMember = repo.getProjectMemberByComposedId(projectId, member.userId).getOrThrow()
                    assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, actualMember.role)

                    repo.updateProjectMemberRole(projectId, actualMember.userId, MemberRole.MEMBER_ROLE_ADMIN)
                }

                val projectAdmins = repo.getAllProjectAdmins(projectId)

                projectAdmins.forEachIndexed { index, admin ->
                    assertEquals(projectId, admin.projectId)
                    assertEquals(members[index].userId, admin.userId)
                    assertEquals(MemberRole.MEMBER_ROLE_ADMIN, admin.role)
                }
            }

        @Test
        fun `When a project member is not project admin, then they are not part of the list of project admins`() =
            runTest {
                val (projectId, members) = setupProject(1)
                members.forEach { member ->
                    val actualMember = repo.getProjectMemberByComposedId(projectId, member.userId).getOrThrow()
                    assertEquals(MemberRole.MEMBER_ROLE_DEFAULT, actualMember.role)
                }

                val projectAdmin = repo.updateProjectMemberRole(
                    projectId,
                    members.first().userId,
                    MemberRole.MEMBER_ROLE_ADMIN,
                )

                val projectAdmins = repo.getAllProjectAdmins(projectId)

                assertThat(projectAdmins).hasSize(1)
                assertThat(projectAdmins).containsExactlyInAnyOrder(projectAdmin)
                assertThat(projectAdmins).doesNotContain(members.last())
            }

        @Test
        fun `When a user is project admin but soft-deleted, then they are not part of the list of project admins`() =
            runTest {
                val (projectId, members) = setupProject(addTestUser = true, addDeletedUser = true)

                val projectAdmin = repo.updateProjectMemberRole(
                    projectId,
                    members[0].userId,
                    MemberRole.MEMBER_ROLE_ADMIN,
                )
                val deletedAdmin = repo.updateProjectMemberRole(
                    projectId,
                    members[1].userId,
                    MemberRole.MEMBER_ROLE_ADMIN,
                )

                val projectAdmins = repo.getAllProjectAdmins(projectId)

                assertThat(projectAdmins).hasSize(1)
                assertThat(projectAdmins).containsExactlyInAnyOrder(projectAdmin)
                assertThat(projectAdmins).doesNotContain(deletedAdmin)
            }
    }

    @Nested
    inner class UpdateProjectMemberRole {
        @ParameterizedTest(name = "When role changes from {0} to {1}, then the final role should be {2}")
        @CsvSource(
            "MEMBER_ROLE_DEFAULT, MEMBER_ROLE_DEFAULT, MEMBER_ROLE_DEFAULT",
            "MEMBER_ROLE_DEFAULT, MEMBER_ROLE_ADMIN, MEMBER_ROLE_ADMIN",
            "MEMBER_ROLE_ADMIN, MEMBER_ROLE_DEFAULT, MEMBER_ROLE_DEFAULT",
            "MEMBER_ROLE_ADMIN, MEMBER_ROLE_ADMIN, MEMBER_ROLE_ADMIN",
        )
        fun `When a project member is updated, then the role of the project member is correctly updated`(
            initialRole: MemberRole,
            updatedRole: MemberRole,
            expectedRole: MemberRole,
        ) = runTest {
            val (projectId, members) = setupProject()
            val member = repo.updateProjectMemberRole(projectId, members.first().userId, initialRole)
            assertEquals(initialRole, member.role)

            val updatedMember = repo.updateProjectMemberRole(projectId, member.userId, updatedRole)

            assertEquals(projectId, updatedMember.projectId)
            assertEquals(member.userId, updatedMember.userId)
            assertEquals(expectedRole, updatedMember.role)
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
                assertThat(projectMembersWithUsers).containsExactlyInAnyOrder(ProjectMemberWithUser(normalMember, user))
            }

        @Test
        fun `When not all users are project members, then only the correct project members with users are returned`() =
            runTest {
                val (projectId, members) = setupProject()
                val nonProjectMemberUserId = insertUserAndGetId("test-user@example.com")
                val user = userRepo.getUserById(members.first().userId).getOrThrow()

                val projectMembersWithUsers = repo.getProjectMembersWithUsers(projectId)

                assertThat(projectMembersWithUsers).hasSize(1)
                assertThat(projectMembersWithUsers).anyMatch { it.projectMember == members.first() }
                assertThat(projectMembersWithUsers).anyMatch { it.user == user }
                assertThat(projectMembersWithUsers).noneMatch { it.projectMember.userId == nonProjectMemberUserId }
            }

        @Test
        fun `When a project member exists but the corresponding user is soft-deleted, then they are not returned`() =
            runTest {
                val (projectId, members) = setupProject(addTestUser = true, addDeletedUser = true)
                assertThat(members).hasSize(2)
                val activeUser = userRepo.getUserById(members[0].userId).getOrThrow()
                val deletedUser = userRepo.getUserById(members[1].userId).getOrThrow()

                val projectMembersWithUsers = repo.getProjectMembersWithUsers(projectId)

                assertThat(projectMembersWithUsers).hasSize(1)
                assertThat(projectMembersWithUsers).anyMatch { it.projectMember == members[0] }
                assertThat(projectMembersWithUsers).anyMatch { it.user == activeUser }
                assertThat(projectMembersWithUsers).noneMatch { it.projectMember == members[1] }
                assertThat(projectMembersWithUsers).noneMatch { it.user == deletedUser }
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

    @Nested
    inner class IsProjectMember {
        @Test
        fun `When the user is a project member, then true is returned`() = runTest {
            val (projectId, members) = setupProject()

            val isMember = repo.isProjectMember(projectId, members[0].userId)

            assertTrue(isMember)
        }

        @Test
        fun `When the user is not a project member, then false is returned`() = runTest {
            val (projectId, _) = setupProject()
            val nonMemberUserId = insertUserAndGetId(email = "non-member-user@example.com")

            val isMember = repo.isProjectMember(projectId, nonMemberUserId)

            assertFalse(isMember)
        }

        @Test
        fun `When the user is soft-deleted, then false is returned`() = runTest {
            val (projectId, members) = setupProject(addTestUser = false, addDeletedUser = true)

            val isMember = repo.isProjectMember(projectId, members[0].userId)

            assertFalse(isMember)
        }
    }
}
