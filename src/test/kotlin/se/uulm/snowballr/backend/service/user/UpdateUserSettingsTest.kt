package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.access.UnauthorizedTestException
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserField
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserSettingsRequest
import java.util.UUID

class UpdateUserSettingsTest : UserServiceTest() {
    private fun getExampleRequest(user: User) = UpdateUserSettingsRequest.fromUser(user)

    @Test
    fun `When a user updates their own settings, then it succeeds`() = runTest {
        val user = DataBuilder.createExampleUser()

        val request = getExampleRequest(user)
        val fields = UserField.entries.filter { it.isSettingsField }.toSet()

        mockCurrentUser(user)
        coEvery { fetcherManagerMock.getAvailableFetchers() } returns emptySet()
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

        val criteria = listOf(
            DataBuilder.createExampleUserCriterion(createdBy = user.id),
            DataBuilder.createExampleUserCriterion(createdBy = user.id),
        )
        val criteriaIds = criteria.map { it.id }
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
        val fields = UserField.entries.filter { it.isSettingsField }.toSet()

        mockCurrentUser(user)
        criteria.forEach { criterion ->
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
            coJustRun { criterionAccessCheckerMock.isAllowedToUseAsDefaultCriterion(user, criterion) }
        }
        coEvery { fetcherManagerMock.getAvailableFetchers() } returns setOf(
            FetcherInformationWithId(id = "foo", information = DataBuilder.createExampleFetcherInformation()),
        )
        coEvery { userRepoMock.updateUser(expectedUser, fields) } returns expectedUser
        coEvery { criterionRepoMock.getCriteriaByIds(criteriaIds) } returns criteria

        val updatedUserSettings = service.updateUserSettings(request, UserField.entries.toSet())

        assertEquals(expectedUser.settings, updatedUserSettings.settings)
        assertEquals(criteria, updatedUserSettings.criteria)
    }

    @Test
    fun `When a user sets a default criterion they are not allowed to use, then the update is rejected`() = runTest {
        val user = DataBuilder.createExampleUser()
        val foreignCriterion = DataBuilder.createExampleUserCriterion()
        val request = getExampleRequest(user).copy(criteriaIds = listOf(foreignCriterion.id))

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(foreignCriterion.id) } returns Result.success(foreignCriterion)
        coEvery {
            criterionAccessCheckerMock.isAllowedToUseAsDefaultCriterion(user, foreignCriterion)
        } throws UnauthorizedTestException()

        assertThrows<UnauthorizedTestException> {
            service.updateUserSettings(request, setOf(UserField.CRITERIA_IDS))
        }

        coVerify(exactly = 0) { userRepoMock.updateUser(any(), any()) }
    }

    @Test
    fun `When a user sets a non-existent default criterion, then the update is rejected`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterionId = UUID.randomUUID()
        val request = getExampleRequest(user).copy(criteriaIds = listOf(criterionId))

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> {
            service.updateUserSettings(request, setOf(UserField.CRITERIA_IDS))
        }

        coVerify(exactly = 0) { userRepoMock.updateUser(any(), any()) }
    }

    @Test
    fun `When the default criteria are not in the update fields, then they are not checked`() = runTest {
        val user = DataBuilder.createExampleUser()
        val request = getExampleRequest(user).copy(criteriaIds = listOf(UUID.randomUUID()))
        val fields = setOf(UserField.ARE_HOTKEYS_SHOWN)

        mockCurrentUser(user)
        coEvery { userRepoMock.updateUser(any(), fields) } returns user
        coEvery { criterionRepoMock.getCriteriaByIds(user.settings.criteriaIds) } returns emptyList()

        service.updateUserSettings(request, fields)

        coVerify(exactly = 0) { criterionRepoMock.getCriterionById(any()) }
    }

    @Test
    fun `When a user updates their default fetchers, then the fetchers are sanitized`() = runTest {
        val user = DataBuilder.createExampleUser()
        val availableFetchers = setOf(
            FetcherInformationWithId(
                id = "existent-fetcher",
                information = DataBuilder.createExampleFetcherInformation(
                    optionSchema = mapOf(
                        "existent-option" to DataBuilder.createExampleFetcherOptionsSchema(),
                    ),
                ),
            ),
        )
        val request = getExampleRequest(user).copy(
            fetchers = mapOf(
                "existent-fetcher" to mapOf(
                    "existent-option" to "foo",
                    "non-existent-option" to "bar",
                ),
                "non-existent-fetcher" to emptyMap(),
            ),
        )
        val fields = setOf(UserField.FETCHERS)

        mockCurrentUser(user)
        coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers
        val updateData = slot<User>()
        coEvery { userRepoMock.updateUser(capture(updateData), fields) } answers { updateData.captured }
        coEvery { criterionRepoMock.getCriteriaByIds(user.settings.criteriaIds) } returns emptyList()

        val updatedUserSettings = service.updateUserSettings(request, fields)

        assertEquals(
            mapOf("existent-fetcher" to mapOf("existent-option" to "foo")),
            updatedUserSettings.settings.defaultProjectSettings.fetchers,
        )
    }

    @Test
    fun `When a user updates their default fetchers without a required option, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val availableFetchers = setOf(
                FetcherInformationWithId(
                    id = "fetcher",
                    information = DataBuilder.createExampleFetcherInformation(
                        optionSchema = mapOf(
                            "option1" to DataBuilder.createExampleFetcherOptionsSchema(isRequired = true),
                        ),
                    ),
                ),
            )
            val request = getExampleRequest(user).copy(fetchers = mapOf("fetcher" to emptyMap()))

            mockCurrentUser(user)
            coEvery { fetcherManagerMock.getAvailableFetchers() } returns availableFetchers

            assertThrows<FailedPreconditionException> {
                service.updateUserSettings(request, setOf(UserField.FETCHERS))
            }

            coVerify(exactly = 0) { userRepoMock.updateUser(any(), any()) }
        }
}
