package se.uulm.snowballr.backend.service.project

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
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class GetAllArchivedProjectsForUserTest : MainServiceTest() {
    @BeforeEach
    override fun setUpTest() {
        super.setUpTest()

        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { projectRepoMock.getUserProjects(any()) } throws NotImplementedError()
    }

    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestedUserId.toString())
        .build()

    @Test
    fun `When all archived user projects are retrieved by another non-admin user, then an exception is thrown`() =
        testCoroutine {
            val anotherUser = DataBuilder.createExampleUser(id = UUID.fromString(dummyUserId))
            val userWithProjects = DataBuilder.createExampleUser(id = requestedUserId)

            coEvery { userRepoMock.getUserById(UUID.fromString(dummyUserId)) } returns anotherUser
            coEvery { userRepoMock.getUserById(userWithProjects.id) } returns userWithProjects
            coEvery {
                projectRepoMock.getUserProjects(any(), ProjectStatus.PROJECT_STATUS_ARCHIVED)
            } returns emptyList()

            assertThrows<UnauthorizedException.Single> {
                mainService.getAllArchivedProjectsForUser(
                    getExampleRequest(),
                )
            }
        }

    fun `When a user retrieves its own archived projects, then all projects are returned successfully`() =
        testCoroutine {
            val userWithProjects = DataBuilder.createExampleUser(id = UUID.fromString(dummyUserId))

            coEvery { userRepoMock.getUserById(UUID.fromString(dummyUserId)) } returns userWithProjects
            coEvery { userRepoMock.getUserById(userWithProjects.id) } returns userWithProjects
            coEvery {
                projectRepoMock.getUserProjects(any(), ProjectStatus.PROJECT_STATUS_ARCHIVED)
            } returns emptyList()

            assertDoesNotThrow { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
        }

    @Test
    fun `When the user's archived projects are retrieved by an admin, then all user projects are returned successfully`() =
        testCoroutine {
            val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            coEvery { userRepoMock.getUserById(any()) } returns adminUser
            coEvery {
                projectRepoMock.getUserProjects(any(), ProjectStatus.PROJECT_STATUS_ARCHIVED)
            } returns emptyList()

            assertDoesNotThrow { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
        }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = testCoroutine {
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When retrieving all user's archived projects fails, then an exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        coEvery { userRepoMock.getUserById(any()) } returns adminUser
        coEvery {
            projectRepoMock.getUserProjects(any(), ProjectStatus.PROJECT_STATUS_ARCHIVED)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(getExampleRequest()) }
    }
}
