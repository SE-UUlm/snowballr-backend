package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class GetUserSettingsTest : MainServiceTest() {
    @Test
    fun `When retrieving current user settings fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } returns user
        coEvery { userRepoMock.getUserSettings(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserSettings() }
    }

    @Test
    fun `When retrieving current user settings is correct, but not default criteria exist, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val userSettings = DataBuilder.createExampleUserSettings()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()
            coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings

            assertDoesNotThrow { mainService.getUserSettings() }
        }

    @Test
    fun `When retrieving current user settings is correct and default criteria exist, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleProjectCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { criterionRepoMock.getCriteriaByIds(listOf(criterion.id)) } returns listOf(criterion)
            coEvery { userRepoMock.getUserSettings(user.id) } returns userSettings

            assertDoesNotThrow { mainService.getUserSettings() }
        }
}
