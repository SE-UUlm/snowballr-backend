package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class GetAllDeletedProjectsForUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @BeforeEach
    fun setupTest() {
        every { GrpcContext.getUserIdFromContext() } throws NotImplementedError()
        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { projectRepoMock.getUserProjects(any(), any()) } throws NotImplementedError()
    }

    @Test
    fun `When parsing the ID fails, then an exception is thrown`() = testCoroutine {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()

        assertThrows<InvalidIdException> { mainService.getAllDeletedProjectsForUser(request) }
    }

    @Test
    fun `When retrieving current user ID fails, then an exception is thrown`() = testCoroutine {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllDeletedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then an exception is thrown`() = testCoroutine {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllDeletedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested user fails, then an exception is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllDeletedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When a non-admin retrieves another user's deleted projects, then an unauthorized exception is thrown`() =
        testCoroutine {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser

            assertThrows<UnauthorizedException.Single> { mainService.getAllDeletedProjectsForUser(getExampleRequest()) }
        }

    @Test
    fun `When retrieving deleted projects fails, then an exception is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllDeletedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When deleted projects are retrieved by an admin, then they are returned successfully`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(any()) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser
        coEvery { projectRepoMock.getUserProjects(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getAllDeletedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When a user retrieves its own deleted projects, then they are returned successfully`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id)
        val request = Base.Id.newBuilder().setId(requestedUser.id.toString()).build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.getUserProjects(any(), any()) } returns emptyList()

        assertDoesNotThrow { mainService.getAllDeletedProjectsForUser(request) }
    }
}
