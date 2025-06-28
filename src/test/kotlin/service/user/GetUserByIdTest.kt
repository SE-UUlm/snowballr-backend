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
internal class GetUserByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()
    private var dummyUserUUID = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    @BeforeEach
    fun setup() {
        dummyUserUUID = UUID.fromString(dummyUserId!!)
    }

    @Test
    fun `When the requesting user has an invalid ID, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        dummyUserId = "invalid-UUID"

        assertThrows<InvalidIdException.UUID> { mainService.getUserById(request) }
    }

    @Test
    fun `When the requested user has an invalid ID, then an exception is thrown`() = testCoroutine {
        val request =
            Base.Id
                .newBuilder()
                .setId("invalid-UUID")
                .build()

        assertThrows<InvalidIdException.UUID> { mainService.getUserById(request) }
    }

    @Test
    fun `When requesting the current user fails, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        coEvery { userRepoMock.getUserById(dummyUserUUID) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user has no access, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        val noAccessUser = DataBuilder.createExampleUser()
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns noAccessUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } returns listOf()

        assertThrows<UnauthorizedException.Single.User> { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then the user can be retrieved`() = testCoroutine {
        val request = getExampleRequest()

        // Mock access check
        val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } returns listOf()

        // Mock user retrieval
        coEvery { userRepoMock.getUserById(requestId) } returns DataBuilder.createExampleUser()

        assertDoesNotThrow { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user is the requested user, then the user can be retrieved`() = testCoroutine {
        dummyUserUUID = UUID.randomUUID()
        dummyUserId = dummyUserUUID.toString()

        val request =
            Base.Id
                .newBuilder()
                .setId(dummyUserId)
                .build()

        // Mock access check
        val requestingUser = DataBuilder.createExampleUser(id = UUID.fromString(dummyUserId))
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns requestingUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } returns listOf()

        // Mock user retrieval
        coEvery { userRepoMock.getUserById(requestId) } returns requestingUser

        assertDoesNotThrow { mainService.getUserById(request) }
    }

    @Test
    fun `When the requesting user is in the same project as requested user, then the user can be retrieved`() =
        testCoroutine {
            val request = getExampleRequest()

            val otherUser = DataBuilder.createExampleUser()
            val member = DataBuilder.createExampleProjectMember(userId = otherUser.id)

            // Mock access check
            coEvery { userRepoMock.getUserById(dummyUserUUID) } returns otherUser
            coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } returns listOf(member)

            // Mock user retrieval
            coEvery { userRepoMock.getUserById(requestId) } returns DataBuilder.createExampleUser()

            assertDoesNotThrow { mainService.getUserById(request) }
        }

    @Test
    fun `When an error occurs while the user is retrieved, then an exception is thrown`() = testCoroutine {
        val request = getExampleRequest()

        // Mock access check
        val adminUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } returns listOf()

        // Mock user retrieval
        coEvery { userRepoMock.getUserById(requestId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(request) }
    }
}
