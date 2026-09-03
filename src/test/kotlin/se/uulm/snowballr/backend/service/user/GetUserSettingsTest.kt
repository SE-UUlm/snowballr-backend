package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder

class GetUserSettingsTest : UserServiceTest() {
    @Test
    fun `When retrieving current user settings is correct, but no default criteria exists, then the correct values are returned`() =
        runTest {
            val user = DataBuilder.createExampleUser()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()

            val result = service.getUserSettings()

            assertEquals(user.settings, result.settings)
            assertEquals(0, result.criteria.size)
        }

    @Test
    fun `When retrieving current user settings is correct and default criteria exists, then the correct values are returned`() =
        runTest {
            val criterion = DataBuilder.createExampleUserCriterion()
            val user = DataBuilder.createExampleUser(criteriaIds = listOf(criterion.id))

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) } returns listOf(criterion)

            val result = service.getUserSettings()

            assertEquals(user.settings, result.settings)
            assertEquals(listOf(criterion), result.criteria)
        }
}
