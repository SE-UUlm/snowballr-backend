package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.repository.H2DatabaseTest
import se.uulm.snowballr.backend.repository.ProjectTableRepo
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.testCoroutine
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.MemberRole
import snowballr.UserOuterClass
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class ProjectMemberTableRepoTest : H2DatabaseTest(arrayOf(ProjectTable, ProjectMemberTable), true) {
    private val repo = ProjectMemberTableRepo(db)
    private val projectRepo = ProjectTableRepo(db)

    suspend fun createExampleProject(): Project {
        val request =
            ProjectOuterClass.Project.Create
                .newBuilder()
                .setName("Test Project")
                .build()
        return projectRepo.createProject(request, testUserId)
    }

    suspend fun addTestMember(index: Int, projectId: UUID): ProjectMember {
        val userId =
            db.dbQuery {
                UserTable
                    .insertAndGetId {
                        it[email] = "test.user.$index@example.com"
                        it[firstName] = "Test"
                        it[lastName] = "User"
                        it[role] = UserOuterClass.UserRole.USER_ROLE_DEFAULT
                        it[status] = UserOuterClass.UserStatus.USER_STATUS_ACTIVE
                    }.value
            }
        return repo.addUserToProject(userId, projectId)
    }

    suspend fun setupProject(): Project {
        val project = createExampleProject()
        repo.addUserToProject(testUserId, project.id)

        return project
    }

    @Nested
    inner class AddUserToProject {
        @Test
        fun `When a user is added to a project, then they can be retrieved as project member`() = testCoroutine {
            val project = createExampleProject()

            val member = repo.addUserToProject(testUserId, project.id)

            assertThat(member.userId).isEqualTo(testUserId)
            assertThat(member.projectId).isEqualTo(project.id)
            assertThat(member.role).isEqualTo(MemberRole.MEMBER_ROLE_DEFAULT)
            val members = repo.getProjectMembersOfProject(project.id)
            assertThat(members).hasSize(1)
            assertThat(members).containsExactly(member)
        }

        @Test
        fun `When a user is added to a project twice, then only one member is created`() = testCoroutine {
            val project = createExampleProject()

            val member = repo.addUserToProject(testUserId, project.id)
            val member1 = repo.addUserToProject(testUserId, project.id)

            assertThat(member).isEqualTo(member1)
            val members = repo.getProjectMembersOfProject(project.id)
            assertThat(members).hasSize(1)
            assertThat(members).containsExactly(member)
        }

        @Test
        fun `When a user is added to a non-existing project, then an exception is thrown`() = testCoroutine {
            assertThrows<NotFoundException.Project> {
                repo.addUserToProject(testUserId, UUID.randomUUID())
            }
        }
    }

    @Test
    fun `When a non-existing user is added to a project, then an exception is thrown`() = testCoroutine {
        val project = createExampleProject()

        assertThrows<NotFoundException.User> {
            repo.addUserToProject(UUID.randomUUID(), project.id)
        }
    }
}

@Nested
inner class GetProjectMembersOfProject {
    @Test
    fun `When no members are in a project, then the list is empty`() = testCoroutine {
        val project = createExampleProject()

        val members = repo.getProjectMembersOfProject(project.id)

        assertThat(members).isEmpty()
    }

    @Test
    fun `When a member is in a project, then they are part of the list`() = testCoroutine {
        val project = setupProject()
        val member1 = addTestMember(1, project.id)
        val member2 = addTestMember(2, project.id)

        val members = repo.getProjectMembersOfProject(project.id)

        assertThat(members).hasSize(3)
        val member = members.first()
        assertThat(member.userId).isEqualTo(testUserId)
        assertThat(member1).isEqualTo(members[1])
        assertThat(member2).isEqualTo(members[2])
    }

    @Test
    fun `When several members are in a project, then they are part of the list`() = testCoroutine {
        val project = setupProject()

        val members = repo.getProjectMembersOfProject(project.id)

        assertThat(members).hasSize(1)
        val member = members.first()
        assertThat(member.userId).isEqualTo(testUserId)
    }
}

@Nested
inner class GetProjectMembersInSameProjectsAsUser {
    @Test
    fun `When the user is in no projects, then the list of members is empty`() = testCoroutine {
        val result = repo.getProjectMembersInSameProjectsAsUser(testUserId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `When the user is in a project, then they are not part of the list of members`() = testCoroutine {
        setupProject()

        val result = repo.getProjectMembersInSameProjectsAsUser(testUserId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `When the user is in projects with other users, then all members are part of the list`() = testCoroutine {
        val project1 = setupProject()
        val project2 = setupProject()
        val project3 = setupProject()

        val member1 = addTestMember(1, project1.id)
        val member2 = addTestMember(2, project2.id)
        val member3 = addTestMember(3, project3.id)

        val result = repo.getProjectMembersInSameProjectsAsUser(testUserId)

        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder(member1, member2, member3)
    }
}
}
