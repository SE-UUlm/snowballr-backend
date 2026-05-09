package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException

class GetUserSettingsTest : UserServiceTest() {
    @Test
    fun `When retrieving current user settings fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)
        coEvery { userRepoMock.getUserSettings(user.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getUserSettings() }
    }

    @Test
    fun `When retrieving current user settings is correct, but not default criteria exist, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val userSettings = DataBuilder.createExampleUserSettings()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()
            coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)

            assertDoesNotThrow { service.getUserSettings() }
        }

    @Test
    fun `When retrieving current user settings is correct and default criteria exist, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleProjectCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) } returns listOf(criterion)
            coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)

            assertDoesNotThrow { service.getUserSettings() }
        }
}
