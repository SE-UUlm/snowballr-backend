package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.service.CriterionService
import java.util.UUID

class CreateCriterionTest : CriterionServiceTest() {
    private fun getProjectCriterionRequest(projectId: UUID) = CreateCriterionRequest(
        tag = "Tag",
        name = "Criterion",
        description = "Description",
        category = CriterionCategory.EXCLUSION,
        projectId = projectId,
    )

    private fun getUserCriterionRequest() = CreateCriterionRequest(
        tag = "Tag",
        name = "Criterion",
        description = "Description",
        category = CriterionCategory.EXCLUSION,
        projectId = null,
    )

    @Test
    fun `When a user creates a project criterion and has access, then the created criterion has the correct values`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
            val request = getProjectCriterionRequest(project.id)

            mockCurrentUser(user)
            coJustRun { criterionAccessCheckerMock.isAllowedToCreateCriterion(user, request) }
            coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

            val result = service.createCriterion(request)

            assertCriterionEquality(criterion, result)
        }

    @Test
    fun `When a user creates a project criterion, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val request = getProjectCriterionRequest(project.id)

            mockCurrentUser(user)
            coEvery {
                criterionAccessCheckerMock.isAllowedToCreateCriterion(user, request)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.createCriterion(request) }
        }

    @Test
    fun `When a user creates a user criterion and has access, then the created criterion has the correct values`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            val request = getUserCriterionRequest()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion
            coJustRun { criterionAccessCheckerMock.isAllowedToCreateCriterion(user, request) }
            coEvery { criterionRepoMock.countUserCriteria(user.id) } returns 0

            val result = service.createCriterion(request)

            assertCriterionEquality(criterion, result)
        }

    @Test
    fun `When a user creates a user criterion and has one less than the maximum, then the criterion is created`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)
            val request = getUserCriterionRequest()

            mockCurrentUser(user)
            coJustRun { criterionAccessCheckerMock.isAllowedToCreateCriterion(user, request) }
            coEvery {
                criterionRepoMock.countUserCriteria(user.id)
            } returns CriterionService.MAX_USER_CRITERIA_COUNT - 1L
            coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

            val result = service.createCriterion(request)

            assertCriterionEquality(criterion, result)
        }

    @Test
    fun `When a user creates a user criterion, but already has the maximum, then a FailedPreconditionException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val request = getUserCriterionRequest()

            mockCurrentUser(user)
            coJustRun { criterionAccessCheckerMock.isAllowedToCreateCriterion(user, request) }
            coEvery {
                criterionRepoMock.countUserCriteria(user.id)
            } returns CriterionService.MAX_USER_CRITERIA_COUNT.toLong()

            assertThrows<FailedPreconditionException> { service.createCriterion(request) }
            coVerify(exactly = 0) { criterionRepoMock.createCriterion(any(), any()) }
        }

    @Test
    fun `When a user creates a project criterion, then the user criteria limit is not checked`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
        val request = getProjectCriterionRequest(project.id)

        mockCurrentUser(user)
        coJustRun { criterionAccessCheckerMock.isAllowedToCreateCriterion(user, request) }
        coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

        service.createCriterion(request)

        coVerify(exactly = 0) { criterionRepoMock.countUserCriteria(any()) }
    }
}
