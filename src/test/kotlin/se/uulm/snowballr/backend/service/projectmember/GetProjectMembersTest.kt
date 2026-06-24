package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import java.util.UUID
import kotlin.test.assertEquals

class GetProjectMembersTest : ProjectMemberServiceTest() {
    @Test
    fun `When a user requests the project members and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, user)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns listOf(projectMemberWithUser)

        val projectMembers = service.getProjectMembers(project.id)

        assertEquals(1, projectMembers.membersCount)
        val actualProjectMember = projectMembers.membersList.first()
        assertEquals(projectMember.userId.toString(), actualProjectMember.user.id)
    }

    @Test
    fun `When a user requests the project members, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, projectId) } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getProjectMembers(projectId) }
        }
}
