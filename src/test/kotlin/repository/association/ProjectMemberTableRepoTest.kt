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

    private suspend fun createExampleProject(): Project {
        val request =
            ProjectOuterClass.Project.Create
                .newBuilder()
                .setName("Test Project")
                .build()
        return projectRepo.createProject(request, testUserId)
    }

    private suspend fun addTestMember(index: Int, projectId: UUID): ProjectMember {
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

    private suspend fun setupProject(
        numberOfTestMembers: Int = 0,
        addTestUser: Boolean = true,
    ): Pair<Project, List<ProjectMember>> {
        val project = createExampleProject()
        val members = mutableListOf<ProjectMember>()

        if (addTestUser) {
            repo.addUserToProject(testUserId, project.id).also { members.add(it) }
        }

        for (i in 1..numberOfTestMembers) {
            addTestMember(i, project.id).also { members.add(it) }
        }

        return project to members
    }

    @Nested
    inner class AddUserToProject {
        @Test
        fun `When a user is added to a project, then they can be retrieved as project member`() = testCoroutine {
            val (project, members) = setupProject()
            val member = members.first()

            assertThat(member.userId).isEqualTo(testUserId)
            assertThat(member.projectId).isEqualTo(project.id)
            assertThat(member.role).isEqualTo(MemberRole.MEMBER_ROLE_DEFAULT)

            val actualMembers = repo.getMembersOfProject(project.id)
            assertThat(actualMembers).hasSize(1)
            assertThat(actualMembers).containsExactly(member)
        }

        @Test
        fun `When a user is added to a project twice, then only one member is created`() = testCoroutine {
            val (project, members) = setupProject()
            val member = members.first()
            val member1 = repo.addUserToProject(testUserId, project.id)

            assertThat(member).isEqualTo(member1)

            val actualMembers = repo.getMembersOfProject(project.id)
            assertThat(actualMembers).hasSize(1)
            assertThat(actualMembers).containsExactly(member)
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
inner class GetMembersOfProject {
    @Test
    fun `When no members are in a project, then the list is empty`() = testCoroutine {
        val (project, _) = setupProject(0, false)

        val members = repo.getMembersOfProject(project.id)

        assertThat(members).isEmpty()
    }

    @Test
    fun `When one member is in a project, then they are part of the list`() = testCoroutine {
        val (project, _) = setupProject()

        val actualMembers = repo.getMembersOfProject(project.id)

        assertThat(actualMembers).hasSize(1)
        val member = actualMembers.first()
        assertThat(member.userId).isEqualTo(testUserId)
    }

    @Test
    fun `When several members are in a project, then they are part of the list`() = testCoroutine {
        val (project, members) = setupProject(3)

        val actualMembers = repo.getMembersOfProject(project.id)

        assertThat(actualMembers).hasSize(4)
        assertThat(actualMembers[0].userId).isEqualTo(testUserId)
        assertThat(actualMembers[0]).isEqualTo(members[0])
        assertThat(actualMembers[1]).isEqualTo(members[1])
        assertThat(actualMembers[2]).isEqualTo(members[2])
        assertThat(actualMembers[3]).isEqualTo(members[3])
    }
}
}

@Nested
inner class GetMembersInSameProjectsAsUser {
    @Test
    fun `When the user is in no projects, then the list of members is empty`() = testCoroutine {
        val result = repo.getMembersInSameProjectsAsUser(testUserId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `When the user is in a project, then they are not part of the list of members`() = testCoroutine {
        setupProject()

        val result = repo.getMembersInSameProjectsAsUser(testUserId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `When the user is in projects with other users, then all members are part of the list`() = testCoroutine {
        val project1 = setupProject().first
        val project2 = setupProject().first
        val project3 = setupProject().first

        val member1 = addTestMember(1, project1.id)
        val member2 = addTestMember(2, project2.id)
        val member3 = addTestMember(3, project3.id)

        val result = repo.getMembersInSameProjectsAsUser(testUserId)

        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder(member1, member2, member3)
    }
}
}
