package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
internal class GetUserByEmailTest : MainServiceTest() {
    private val requestEmail = "test.user@example.com"
    private var dummyUserUUID = UUID.randomUUID()

    private fun getExampleRequest() = Base.Email
        .newBuilder()
        .setEmail(requestEmail)
        .build()

    @BeforeEach
    fun setup() {
        dummyUserUUID = UUID.fromString(dummyUserId!!)
    }

    @Test
    fun `When the requesting user has an invalid ID, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        dummyUserId = "invalid-UUID"

        assertThrows<InvalidIdException.UUID> { mainService.getUserByEmail(request) }
    }

    @Test
    fun `When the requesting the current user fails, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        coEvery { userRepoMock.getUserById(dummyUserUUID) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(request) }
    }

    @Test
    fun `When the requesting user has no access, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        val noAccessUser = DataBuilder.createExampleUser()
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns noAccessUser
        coEvery { userRepoMock.getUserByEmail(requestEmail) } returns DataBuilder.createExampleUser()
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        assertThrows<UnauthorizedException.Single.User> { mainService.getUserByEmail(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then the user can be retrieved`() = testCoroutine {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { userRepoMock.getUserByEmail(requestEmail) } returns DataBuilder.createExampleUser()
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        assertDoesNotThrow { mainService.getUserByEmail(request) }
    }

    @Test
    fun `When the requesting user is the requested user, then the user can be retrieved`() = testCoroutine {
        dummyUserUUID = UUID.randomUUID()
        dummyUserId = dummyUserUUID.toString()

        val request = getExampleRequest()

        val requestingUser = DataBuilder.createExampleUser(id = UUID.fromString(dummyUserId))
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns requestingUser
        coEvery { userRepoMock.getUserByEmail(requestEmail) } returns requestingUser
        coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf()

        assertDoesNotThrow { mainService.getUserByEmail(request) }
    }

    @Test
    fun `When the requesting user is in the same project as requested user, then the user can be retrieved`() =
        testCoroutine {
            val request = getExampleRequest()

            val otherUser = DataBuilder.createExampleUser()
            val member = DataBuilder.createExampleProjectMember(userId = otherUser.id)

            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns otherUser
            coEvery { userRepoMock.getUserByEmail(requestEmail) } returns DataBuilder.createExampleUser()
            coEvery { projectMemberRepoMock.getProjectMembersInSameProjectsAsUser(any()) } returns listOf(member)

            assertDoesNotThrow { mainService.getUserByEmail(request) }
        }

    @Test
    fun `When an error occurs while the user is retrieved, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { userRepoMock.getUserByEmail(requestEmail) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(request) }
    }
}
