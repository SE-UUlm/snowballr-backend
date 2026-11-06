package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.repository.FOREIGN_KEY_CONSTRAINT_VIOLATION_SQL_STATE
import se.uulm.snowballr.backend.repository.UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE
import java.sql.SQLException

class DeleteCriterionTest : CriterionServiceTest() {
    @Test
    fun `When retrieving the criterion fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.deleteCriterion(criterion.id) }
        coVerify(exactly = 0) { criterionRepoMock.deleteCriterionById(any()) }
    }

    @Test
    fun `When a user deletes a criterion, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coEvery {
            criterionAccessCheckerMock.isAllowedToDeleteCriterion(user, criterion)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.deleteCriterion(criterion.id) }
        coVerify(exactly = 0) { criterionRepoMock.deleteCriterionById(any()) }
    }

    @Test
    fun `When a project criterion can be deleted, then the criterion is deleted`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(status = ProjectStatus.ACTIVE)
        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToDeleteCriterion(user, criterion) }
        coJustRun { criterionRepoMock.deleteCriterionById(criterion.id) }

        service.deleteCriterion(criterion.id)

        coVerify(exactly = 1) { criterionRepoMock.deleteCriterionById(criterion.id) }
    }

    @Test
    fun `When a user criterion can be deleted, then the criterion is deleted`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToDeleteCriterion(user, criterion) }
        coJustRun { criterionRepoMock.deleteCriterionById(criterion.id) }

        service.deleteCriterion(criterion.id)

        coVerify(exactly = 1) { criterionRepoMock.deleteCriterionById(criterion.id) }
    }

    @Test
    fun `When a criterion is still selected in a review, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToDeleteCriterion(user, criterion) }
        coEvery {
            criterionRepoMock.deleteCriterionById(criterion.id)
        } throws SQLException("FK violation", FOREIGN_KEY_CONSTRAINT_VIOLATION_SQL_STATE)

        assertThrows<FailedPreconditionException> { service.deleteCriterion(criterion.id) }
    }

    @Test
    fun `When deleting a criterion fails for another SQL reason, then the SQLException is rethrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val criterion = DataBuilder.createExampleProjectCriterion()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)
        coJustRun { criterionAccessCheckerMock.isAllowedToDeleteCriterion(user, criterion) }
        coEvery {
            criterionRepoMock.deleteCriterionById(criterion.id)
        } throws SQLException("Other violation", UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE)

        assertThrows<SQLException> { service.deleteCriterion(criterion.id) }
    }
}
