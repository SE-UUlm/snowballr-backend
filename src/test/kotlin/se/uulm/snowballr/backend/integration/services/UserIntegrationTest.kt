package se.uulm.snowballr.backend.integration.services

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.user.UserField
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserSettingsRequest

class UserIntegrationTest : IntegrationTest() {
    @Nested
    inner class Register {
        @Test
        fun `When a user registers, then their status is unconfirmed until they verify their email`() = runTest {
            val newUser = DataBuilder.createExampleUser(email = "new.user@example.com")
            val tokenSlot = slot<String>()

            coEvery { emailManagerMock.createVerificationLink(capture(tokenSlot)) } returns "https://example.com/verify"
            coJustRun { emailManagerMock.sendVerificationEmail(any(), any()) }

            userService.register(
                RegisterRequest(
                    firstName = newUser.firstName,
                    lastName = newUser.lastName,
                    email = newUser.email,
                    password = "SecureP@ssw0rd!",
                ),
            )

            val unverifiedUser = userService.getUserByEmail(newUser.email)
            assertEquals(UserStatus.ACTIVE_UNCONFIRMED, unverifiedUser.status)

            authenticationService.verifyEmail(tokenSlot.captured)

            val verifiedUser = userService.getUserByEmail(newUser.email)
            assertEquals(UserStatus.ACTIVE, verifiedUser.status)
        }
    }

    @Nested
    inner class GetUser {
        @Test
        fun `When the current user requests their own data, then their data is returned`() = runTest {
            val currentUser = userService.getCurrentUser()

            assertEquals(testUserId, currentUser.id)
        }

        @Test
        fun `When an admin requests another user by ID, then the user's data is returned`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            val fetchedUser = userService.getUserById(otherUser.id)

            assertEquals(otherUser.id, fetchedUser.id)
            assertEquals(otherUser.email, fetchedUser.email)
        }

        @Test
        fun `When an admin requests all users, then all users are included in the response`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            val allUsers = userService.getAllUsers()
            val userIds = allUsers.map { it.id }

            assertEquals(2, allUsers.size)
            assertTrue(userIds.contains(testUserId))
            assertTrue(userIds.contains(otherUser.id))
        }

        @Test
        fun `When a non-admin requests all users, then access is denied`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            actAsUser(otherUser.id) {
                assertThrows<UnauthorizedReadAllException> { userService.getAllUsers() }
            }
        }
    }

    @Nested
    inner class UpdateUser {
        @Test
        fun `When a user updates their own first name, then the updated name is persisted`() = runTest {
            val currentUser = userService.getCurrentUser()
            val newFirstName = "UpdatedFirstName"

            val request = UpdateUserRequest(
                userId = currentUser.id,
                firstName = newFirstName,
                lastName = currentUser.lastName,
                email = currentUser.email,
                role = currentUser.role,
                status = currentUser.status,
            )

            val updatedUser = userService.updateUser(request, setOf(UserField.FIRST_NAME))

            assertEquals(newFirstName, updatedUser.firstName)
        }

        @Test
        fun `When an admin updates another user's first name, then the updated name is persisted`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val newFirstName = "AdminUpdatedName"

            val request = UpdateUserRequest(
                userId = otherUser.id,
                firstName = newFirstName,
                lastName = otherUser.lastName,
                email = otherUser.email,
                role = otherUser.role,
                status = otherUser.status,
            )

            val updatedUser = userService.updateUser(request, setOf(UserField.FIRST_NAME))

            assertEquals(newFirstName, updatedUser.firstName)
        }

        @Test
        fun `When a non-admin user tries to escalate their own role, then an UnauthorizedUpdateException is thrown`() =
            runTest {
                val nonAdminUser = addUser(DataBuilder.createExampleUser(email = "non.admin@example.com"))

                val request = UpdateUserRequest(
                    userId = nonAdminUser.id,
                    firstName = nonAdminUser.firstName,
                    lastName = nonAdminUser.lastName,
                    email = nonAdminUser.email,
                    role = UserRole.ADMIN,
                    status = nonAdminUser.status,
                )

                actAsUser(nonAdminUser.id) {
                    assertThrows<UnauthorizedUpdateException> {
                        userService.updateUser(request, setOf(UserField.ROLE))
                    }
                }
            }

        @Test
        fun `When a user tries to change their email to one already in use, then a DuplicateUserException is thrown`() =
            runTest {
                val existingUser = addUser(DataBuilder.createExampleUser(email = "existing@example.com"))
                val currentUser = userService.getCurrentUser()

                val request = UpdateUserRequest(
                    userId = currentUser.id,
                    firstName = existingUser.firstName,
                    lastName = existingUser.lastName,
                    email = existingUser.email,
                    role = existingUser.role,
                    status = existingUser.status,
                )

                assertThrows<DuplicateUserException> { userService.updateUser(request, setOf(UserField.EMAIL)) }
            }
    }

    @Nested
    inner class UpdateUserSettings {
        private val settingsFields = UserField.entries.filter { it.isSettingsField() }.toSet()

        private suspend fun currentSettingsRequest() = UpdateUserSettingsRequest.fromUser(userService.getCurrentUser())

        @Test
        fun `When a user updates all of their settings, then the updated settings are persisted`() = runTest {
            val request = currentSettingsRequest().copy(
                areHotkeysShown = true,
                isReviewModeEnabled = true,
                similarityThreshold = 0.75F,
                snowballingType = SnowballingType.FORWARD,
                reviewMaybeAllowed = true,
                decisionMatrix = ReviewDecisionMatrix(
                    numberOfReviewers = 3,
                    patterns = listOf(DecisionMatrixPattern(PaperDecision.ACCEPTED, emptyList())),
                ),
                fetchers = mapOf("crossref" to emptyMap()),
            )

            val updated = userService.updateUserSettings(request, settingsFields)

            val persisted = userService.getUserSettings().settings
            assertEquals(persisted, updated.settings)
            assertTrue(persisted.areHotkeysShown)
            assertTrue(persisted.isReviewModeEnabled)
            assertEquals(0.75F, persisted.defaultProjectSettings.similarityThreshold)
            assertEquals(SnowballingType.FORWARD, persisted.defaultProjectSettings.snowballingType)
            assertTrue(persisted.defaultProjectSettings.reviewMaybeAllowed)
            assertEquals(mapOf("crossref" to emptyMap<String, String>()), persisted.defaultProjectSettings.fetchers)
            assertEquals(3, persisted.defaultProjectSettings.reviewDecisionMatrix.numberOfReviewers)
            assertEquals(
                listOf(DecisionMatrixPattern(PaperDecision.ACCEPTED, emptyList())),
                persisted.defaultProjectSettings.reviewDecisionMatrix.patterns,
            )
        }

        @Test
        fun `When a user sets default criteria in their settings, then the criteria are returned with the settings`() =
            runTest {
                val criterion = criterionService.createCriterion(
                    CreateCriterionRequest(
                        tag = "UC",
                        name = "User Criterion",
                        description = "A default user criterion",
                        category = CriterionCategory.INCLUSION,
                        projectId = null,
                    ),
                )

                val request = currentSettingsRequest().copy(criteriaIds = listOf(criterion.id))

                val updated = userService.updateUserSettings(request, settingsFields)

                assertEquals(listOf(criterion.id), updated.settings.criteriaIds)
                assertEquals(listOf(criterion.id), updated.criteria.map { it.id })
                assertEquals(listOf(criterion.id), userService.getUserSettings().criteria.map { it.id })
            }

        @Test
        fun `When only a subset of settings fields is requested, then fields outside the subset are not modified`() =
            runTest {
                val original = userService.getUserSettings().settings
                val request = currentSettingsRequest().copy(
                    areHotkeysShown = !original.areHotkeysShown,
                    similarityThreshold = 0.123F,
                )

                userService.updateUserSettings(request, setOf(UserField.ARE_HOTKEYS_SHOWN))

                val persisted = userService.getUserSettings().settings
                assertEquals(!original.areHotkeysShown, persisted.areHotkeysShown)
                assertEquals(
                    original.defaultProjectSettings.similarityThreshold,
                    persisted.defaultProjectSettings.similarityThreshold,
                )
            }

        @Test
        fun `When non-settings fields are included in the requested fields, then they are ignored`() = runTest {
            val originalEmail = userService.getCurrentUser().email
            val request = currentSettingsRequest().copy(isReviewModeEnabled = true)

            val updated = userService.updateUserSettings(
                request,
                setOf(UserField.EMAIL, UserField.ROLE, UserField.IS_REVIEW_MODE_ENABLED),
            )

            assertTrue(updated.settings.isReviewModeEnabled)
            assertEquals(originalEmail, userService.getCurrentUser().email)
        }

        @Test
        fun `When another user updates their settings, then the current user's settings are unaffected`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.settings@example.com"))
            val ownSettingsBefore = userService.getUserSettings().settings

            actAsUser(otherUser.id) {
                val request = UpdateUserSettingsRequest.fromUser(userService.getCurrentUser()).copy(
                    areHotkeysShown = !ownSettingsBefore.areHotkeysShown,
                    isReviewModeEnabled = true,
                )

                userService.updateUserSettings(request, settingsFields)

                assertTrue(userService.getUserSettings().settings.isReviewModeEnabled)
            }

            assertEquals(ownSettingsBefore, userService.getUserSettings().settings)
        }
    }

    @Nested
    inner class DeleteUser {
        @Test
        fun `When an admin soft-deletes a non-admin user, then the operation succeeds`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            assertDoesNotThrow { userService.softDeleteUser(otherUser.id) }
        }

        @Test
        fun `When a user soft-deletes themselves, then the operation succeeds`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            actAsUser(otherUser.id) {
                assertDoesNotThrow { userService.softDeleteUser(otherUser.id) }
            }
        }
    }
}
