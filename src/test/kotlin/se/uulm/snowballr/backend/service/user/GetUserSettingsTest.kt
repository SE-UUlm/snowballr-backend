package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import kotlin.test.assertEquals

class GetUserSettingsTest : UserServiceTest() {
    @Test
    fun `When retrieving current user settings fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)
        coEvery { userRepoMock.getUserSettings(user.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getUserSettings() }
    }

    @Test
    fun `When retrieving current user settings is correct, but no default criteria exists, then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val userSettings = DataBuilder.createExampleUserSettings()

            mockCurrentUser(user)
            coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)
            coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()

            val result = service.getUserSettings()

            assertUserSettingsEquality(userSettings, result.settings)
            assertEquals(emptyList(), result.criteria)
        }

    @Test
    fun `When retrieving current user settings is correct and default criteria exists, then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))

            mockCurrentUser(user)
            coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)
            coEvery { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) } returns listOf(criterion)

            val result = service.getUserSettings()

            assertUserSettingsEquality(userSettings, result.settings)
            assertEquals(listOf(criterion), result.criteria)
        }
}
