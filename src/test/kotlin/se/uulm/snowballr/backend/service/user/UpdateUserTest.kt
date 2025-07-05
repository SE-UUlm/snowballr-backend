package se.uulm.snowballr.backend.service.user

import com.google.protobuf.util.FieldMaskUtil
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
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.UserOuterClass.User
import snowballr.UserOuterClass.UserRole
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class UpdateUserTest : MainServiceTest() {
    @BeforeEach
    override fun setUpTest() {
        super.setUpTest()

        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { userRepoMock.doesUserExistByEmail(any()) } throws NotImplementedError()
        coEvery { userRepoMock.updateUser(any()) } throws NotImplementedError()
    }

    @Test
    fun `When a user updates their own information successfully, then no exception is thrown`() = testCoroutine {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val updatedUser = DataBuilder.createExampleUser(
            id = nonAdminUser.id,
            firstName = "John",
            lastName = "Doe",
        )
        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("first_name", "last_name"))
        val request = User.Update.newBuilder().setUser(updatedUser.toGrpcUser()).setMask(updateFieldMask).build()

        coEvery { userRepoMock.getUserById(any()) } returns nonAdminUser
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.updateUser(request) } returns updatedUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When an admin updates a user's information successfully, then no exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val updatedUser = DataBuilder.createExampleUser(
            email = "john@doe.com",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.USER_ROLE_ADMIN,
        )
        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("email", "first_name", "last_name", "role"))
        val request = User.Update.newBuilder().setUser(updatedUser.toGrpcUser()).setMask(updateFieldMask).build()

        coEvery { userRepoMock.getUserById(any()) } returns adminUser
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.updateUser(request) } returns updatedUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When a user wants to change their email to an existing email, then an exception is thrown`() = testCoroutine {
        val user = DataBuilder.createExampleUser()
        val request = User.Update.newBuilder().setUser(
            user.toGrpcUser(),
        ).setMask(FieldMaskUtil.fromString("email")).build()

        coEvery { userRepoMock.getUserById(any()) } returns user
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns true

        assertThrows<SnowballRException.DuplicateEntityException> { mainService.updateUser(request) }
    }

    @Test
    fun `When a non-admin user wants to change the user role, then an exception is thrown`() = testCoroutine {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val updatedUser = DataBuilder.createExampleUser().toGrpcUser()
        val request = User.Update.newBuilder().setUser(updatedUser).setMask(FieldMaskUtil.fromString("role")).build()

        coEvery { userRepoMock.getUserById(any()) } returns nonAdminUser

        assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateUser(request) }
    }

    @Test
    fun `When a non-admin user wants to change the user information from another user, then an exception is thrown`() =
        testCoroutine {
            val anotherUser = DataBuilder.createExampleUser(id = UUID.fromString(dummyUserId))
            val updatedUser = DataBuilder.createExampleUser()
            val updateFieldMask = FieldMaskUtil.fromString("first_name")
            val request = User.Update.newBuilder().setUser(updatedUser.toGrpcUser()).setMask(updateFieldMask).build()

            coEvery { userRepoMock.getUserById(UUID.fromString(dummyUserId)) } returns anotherUser
            coEvery { userRepoMock.getUserById(updatedUser.id) } returns updatedUser

            assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateUser(request) }
        }

    @Test
    fun `When an error occurs while updating a user, then an exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val updatedUser = DataBuilder.createExampleUser().toGrpcUser()
        val request = User.Update.newBuilder().setUser(updatedUser).build()

        coEvery { userRepoMock.getUserById(any()) } returns adminUser
        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.updateUser(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(request) }
    }
}
