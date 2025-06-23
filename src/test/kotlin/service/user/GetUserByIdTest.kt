package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class GetUserByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID().toString()

    @Test
    fun `When the requesting the current user fails, then an exception is thrown`() = testCoroutine {
        val request = Base.Id.newBuilder().build()

        coEvery { userRepoMock.getUserById(dummyUserId!!) } throws Exception("Failed to retrieve user")

        assertThrows<Exception> { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user has no access, then an exception is thrown`() = testCoroutine {
        val request =
            Base.Id
                .newBuilder()
                .setId(requestId)
                .build()

        val noAccessUser = DataBuilder.createExampleUser()
        coEvery { userRepoMock.getUserById(dummyUserId!!) } returns noAccessUser
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        assertThrows<UnauthorizedException.Single.User> { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then the user can be retrieved`() = testCoroutine {
        val request =
            Base.Id
                .newBuilder()
                .setId(requestId)
                .build()

        // Mock access check
        val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        coEvery { userRepoMock.getUserById(dummyUserId!!) } returns adminUser
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        // Mock user retrieval
        coEvery { userRepoMock.getUserById(requestId) } returns DataBuilder.createExampleUser()

        assertDoesNotThrow { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user is the requested user, then the user can be retrieved`() = testCoroutine {
        dummyUserId = UUID.randomUUID().toString()

        val request =
            Base.Id
                .newBuilder()
                .setId(dummyUserId)
                .build()

        // Mock access check
        val requestingUser = DataBuilder.createExampleUser(id = UUID.fromString(dummyUserId))
        coEvery { userRepoMock.getUserById(dummyUserId!!) } returns requestingUser
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        // Mock user retrieval
        coEvery { userRepoMock.getUserById(requestId) } returns requestingUser

        assertDoesNotThrow { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user is in the same project as requested user, then the user can be retrieved`() =
        testCoroutine {
            val request =
                Base.Id
                    .newBuilder()
                    .setId(requestId)
                    .build()

            val otherUser = DataBuilder.createExampleUser()
            val member = DataBuilder.createExampleProjectMember(userId = otherUser.id)

            // Mock access check
            coEvery { userRepoMock.getUserById(dummyUserId!!) } returns otherUser
            coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf(member)

            // Mock user retrieval
            coEvery { userRepoMock.getUserById(requestId) } returns DataBuilder.createExampleUser()

            assertDoesNotThrow { mainService.getUserById(request) }
        }

    @Test
    fun `When an error occurs while the user is retrieved, then an exception is thrown`() = testCoroutine {
        val request =
            Base.Id
                .newBuilder()
                .setId(requestId)
                .build()

        // Mock access check
        val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        coEvery { userRepoMock.getUserById(dummyUserId!!) } returns adminUser
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        // Mock user retrieval
        coEvery { userRepoMock.getUserById(requestId) } throws Exception("Failed to retrieve user")

        assertThrows<Exception> { mainService.getUserById(request) }
    }
}
