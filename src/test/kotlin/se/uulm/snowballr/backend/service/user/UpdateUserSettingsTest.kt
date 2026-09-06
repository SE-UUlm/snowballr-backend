package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserField
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserSettingsRequest
import java.util.UUID

class UpdateUserSettingsTest : UserServiceTest() {
    private fun getExampleRequest(user: User) = UpdateUserSettingsRequest.fromUser(user)

    @Test
    fun `When a user updates their own settings, then it succeeds`() = runTest {
        val user = DataBuilder.createExampleUser()

        val request = getExampleRequest(user)
        val fields = UserField.entries.filter { it.isSettingsField() }.toSet()

        mockCurrentUser(user)
        coEvery { userRepoMock.updateUser(user, fields) } returns user
        coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()

        val updatedUserSettings = service.updateUserSettings(request, UserField.entries.toSet())

        // no-op update
        assertEquals(user.settings, updatedUserSettings.settings)
        assertEquals(emptyList<Criterion>(), updatedUserSettings.criteria)

        // updateUser is called with subset of requested fields
        coVerify(exactly = 1) { userRepoMock.updateUser(user, fields) }
    }

    @Test
    fun `When all user settings are updated, then the returned values match the request`() = runTest {
        val user = DataBuilder.createExampleUser()

        val criteriaIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val expectedUser = user.copy(
            settings = user.settings.copy(
                areHotkeysShown = false,
                isReviewModeEnabled = true,
                criteriaIds = criteriaIds,
                defaultProjectSettings = ProjectSettings(
                    similarityThreshold = 0.66F,
                    reviewDecisionMatrix = ReviewDecisionMatrix(
                        numberOfReviewers = 5,
                        patterns = listOf(
                            DecisionMatrixPattern(
                                PaperDecision.ACCEPTED,
                                emptyList(),
                            ),
                        ),
                    ),
                    fetchers = mapOf(
                        "foo" to emptyMap(),
                    ),
                    snowballingType = SnowballingType.FORWARD,
                    reviewMaybeAllowed = false,
                ),
            ),
        )
        val request = UpdateUserSettingsRequest.fromUser(expectedUser)
        val fields = UserField.entries.filter { it.isSettingsField() }.toSet()

        mockCurrentUser(user)
        coEvery { userRepoMock.updateUser(expectedUser, fields) } returns expectedUser
        coEvery { criterionRepoMock.getCriteriaByIds(criteriaIds) } returns emptyList()

        val updatedUserSettings = service.updateUserSettings(request, UserField.entries.toSet())

        assertEquals(expectedUser.settings, updatedUserSettings.settings)
    }
}
