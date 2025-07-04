package se.uulm.snowballr.backend.service.user

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.UserOuterClass.User
import snowballr.UserOuterClass.UserRole

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class UpdateUserTest : MainServiceTest() {
    @Test
    fun `When a user updates their own information successfully, then no exception is thrown`() = testCoroutine {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val updatedUser = DataBuilder.createExampleUser(
            id = nonAdminUser.id,
            firstName = "John",
            lastName = "Doe",
        ).toGrpcUser()
        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("first_name", "last_name"))
        val request = User.Update.newBuilder().setUser(updatedUser).setMask(updateFieldMask).build()

        coEvery { userRepoMock.getUserById(any()) } returns nonAdminUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When an admin updates a user's information successfully, then no exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val updatedUser = DataBuilder.createExampleUser(
            email = "john@doe.com",
            firstName = "John",
            lastName = "Doe",
        ).toGrpcUser()
        val updateFieldMask = FieldMaskUtil.fromStringList(listOf("email", "first_name", "last_name"))
        val request = User.Update.newBuilder().setUser(updatedUser).setMask(updateFieldMask).build()

        coEvery { userRepoMock.getUserById(any()) } returns adminUser

        assertDoesNotThrow { mainService.updateUser(request) }
    }

    @Test
    fun `When a user wants to change their email to an existing email, then an exception is thrown`() = testCoroutine {
        val request = User.Update.newBuilder().setMask(FieldMaskUtil.fromString("email")).build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns true

        assertThrows<SnowballRException.DuplicateEntityException> { mainService.updateUser(request) }
    }

    @Test
    fun `When a non-admin user wants to change the user role, then an exception is thrown`() = testCoroutine {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val request = User.Update.newBuilder().setMask(FieldMaskUtil.fromString("role")).build()

        coEvery { userRepoMock.getUserById(any()) } returns nonAdminUser

        assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateUser(request) }
    }

    @Test
    fun `When a non-admin user wants to change the user information from another user, then an exception is thrown`() =
        testCoroutine {
            val anotherUser = DataBuilder.createExampleUser()
            val updatedUser = DataBuilder.createExampleUser().toGrpcUser()
            val updateFieldMask = FieldMaskUtil.fromString("first_name")
            val request = User.Update.newBuilder().setUser(updatedUser).setMask(updateFieldMask).build()

            coEvery { userRepoMock.getUserById(any()) } returns anotherUser

            assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.updateUser(request) }
        }

    @Test
    fun `When an error occurs while updating a user, then an exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val request = User.Update.newBuilder().build()

        coEvery { userRepoMock.getUserById(any()) } returns adminUser
        coEvery { userRepoMock.updateUser(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(request) }
    }
}
