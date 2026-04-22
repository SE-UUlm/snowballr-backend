package se.uulm.snowballr.backend.access.rules

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import snowballr.ProjectOuterClass.ProjectStatus

class ProjectAccessRuleTest {
    private val requester = DataBuilder.createExampleUser()

    @Nested
    inner class IsProjectActive {
        @Test
        fun `When the project status is ACTIVE, then no exception is thrown`() = runTest {
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE)

            assertDoesNotThrow { isProjectActive().checkFor(requester, project) }
        }

        @Test
        fun `When the project status is ACTIVE_LOCKED, then no exception is thrown`() = runTest {
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

            assertDoesNotThrow { isProjectActive().checkFor(requester, project) }
        }

        @Test
        fun `When the project status is ARCHIVED, then an EntityNotActiveException is thrown`() = runTest {
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)

            assertThrows<EntityNotActiveException> { isProjectActive().checkFor(requester, project) }
        }

        @Test
        fun `When the project status is DELETED, then an EntityNotActiveException is thrown`() = runTest {
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_DELETED)

            assertThrows<EntityNotActiveException> { isProjectActive().checkFor(requester, project) }
        }
    }
}
