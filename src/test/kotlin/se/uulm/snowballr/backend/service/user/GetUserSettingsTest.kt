package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest

class GetUserSettingsTest : MainServiceTest() {
    @Test
    fun `When retrieving current user settings fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { userRepoMock.getUserSettings(user.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserSettings() }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
        coVerify(exactly = 1) { userRepoMock.getUserSettings(user.id) }
        coVerify(exactly = 0) { criterionRepoMock.getCriteriaByIds(any()) }
    }

    @Test
    fun `When retrieving current user settings is correct, but not default criteria exist, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val userSettings = DataBuilder.createExampleUserSettings()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings
            coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()

            assertDoesNotThrow { mainService.getUserSettings() }

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
            coVerify(exactly = 1) { userRepoMock.getUserSettings(user.id) }
            coVerify(exactly = 1) { criterionRepoMock.getCriteriaByIds(emptyList()) }
        }

    @Test
    fun `When retrieving current user settings is correct and default criteria exist, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleProjectCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings
            coEvery { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) } returns listOf(criterion)

            assertDoesNotThrow { mainService.getUserSettings() }

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
            coVerify(exactly = 1) { userRepoMock.getUserSettings(user.id) }
            coVerify(exactly = 1) { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) }
        }
}
