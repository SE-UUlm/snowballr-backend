package se.uulm.snowballr.backend.service.user

import com.google.protobuf.FieldMask
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID
import snowballr.UserOuterClass.User as GrpcUser

class UpdateUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private val requestEmail = "newemail@example.com"

    private fun getExampleRequest(changeRole: Boolean = false): GrpcUser.Update {
        val userBuilder = GrpcUser.newBuilder()
            .setId(requestedUserId.toString())
            .setEmail(requestEmail)

        if (changeRole) {
            userBuilder.setRole(UserRole.USER_ROLE_ADMIN)
        }

        val mask = FieldMask.newBuilder()
            .addPaths("email")
            .addPaths("role")
            .build()

        return GrpcUser.Update.newBuilder()
            .setUser(userBuilder.build())
            .setMask(mask)
            .build()
    }

    @Test
    fun `When requested user retrieval fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When user is not admin and tries to change another user's role, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)

            assertThrows<UnauthorizedException> { mainService.updateUser(getExampleRequest(true)) }
        }

    @Test
    fun `When user is not admin and tries to update another user, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val otherUser = DataBuilder.createExampleUser(id = requestedUserId)
        val request = GrpcUser.Update.newBuilder()
            .setUser(GrpcUser.newBuilder().setId(otherUser.id.toString()).setFirstName("NewFirstName"))
            .setMask(FieldMask.newBuilder().addPaths("first_name"))
            .build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)

        assertThrows<UnauthorizedException> { mainService.updateUser(request) }
    }

    @Test
    fun `When updating email to existent email, then a DuplicateEntityException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(
            id = requestedUserId,
            email = requestEmail,
            status = UserStatus.USER_STATUS_ACTIVE,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)
        coEvery { userRepoMock.doesUserExistByEmail(requestedUser.email) } returns true

        assertThrows<DuplicateEntityException> { mainService.updateUser(getExampleRequest()) }
    }

    @Test
    fun `When user updates own email, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_DEFAULT)
        val request = GrpcUser.Update.newBuilder()
            .setUser(
                GrpcUser.newBuilder()
                    .setId(currentUser.id.toString())
                    .setEmail("new-and-shiny@example.com"),
            )
            .setMask(FieldMask.newBuilder().addPaths("email"))
            .build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.doesUserExistByEmail("new-and-shiny@example.com") } returns false
        coEvery { userRepoMock.updateUser(request) } returns currentUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When admin updates another user's role, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val otherUser = DataBuilder.createExampleUser(
            id = requestedUserId,
            role = UserRole.USER_ROLE_DEFAULT,
            status = UserStatus.USER_STATUS_ACTIVE,
        )
        val request = GrpcUser.Update.newBuilder()
            .setUser(
                GrpcUser.newBuilder()
                    .setId(otherUser.id.toString())
                    .setRole(UserRole.USER_ROLE_ADMIN),
            )
            .setMask(FieldMask.newBuilder().addPaths("role"))
            .build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coEvery { userRepoMock.updateUser(request) } returns otherUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When admin updates all fields of another user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(
            id = requestedUserId,
            email = requestEmail,
            status = UserStatus.USER_STATUS_ACTIVE,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)
        coEvery { userRepoMock.doesUserExistByEmail(requestedUser.email) } returns false
        coEvery { userRepoMock.updateUser(getExampleRequest()) } returns requestedUser

        assertDoesNotThrow { mainService.updateUser(getExampleRequest()) }
    }
}
