package se.uulm.snowballr.backend.access.rules

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException

class ProjectAccessRuleTest {
    private val requester = DataBuilder.createExampleUser()

    companion object {
        @JvmStatic
        fun activeStatuses() = listOf(
            ProjectStatus.ACTIVE,
            ProjectStatus.ACTIVE_LOCKED,
        )

        @JvmStatic
        fun inactiveStatuses() = ProjectStatus.entries.filter { !activeStatuses().contains(it) }
    }

    @Nested
    inner class IsProjectActive {
        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.ProjectAccessRuleTest#activeStatuses")
        fun `When the project is active, then no exception is thrown`(status: ProjectStatus) = runTest {
            val project = DataBuilder.createExampleProject(status = status)

            assertDoesNotThrow { isProjectActive().checkFor(requester, project) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.rules.ProjectAccessRuleTest#inactiveStatuses")
        fun `When the project is inactive, then an EntityNotActiveException is thrown`(status: ProjectStatus) =
            runTest {
                val project = DataBuilder.createExampleProject(status = status)

                assertThrows<EntityNotActiveException> { isProjectActive().checkFor(requester, project) }
            }
    }
}
