package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
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
            coJustRun { criterionAccessCheckerMock.isAllowedToCreateProjectCriterion(user, project.id) }
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
                criterionAccessCheckerMock.isAllowedToCreateProjectCriterion(user, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.createCriterion(request) }
        }

    @Test
    fun `When a user creates a user criterion and has access, then the created criterion has the correct values`() =
        runTest {
            val user = DataBuilder.createExampleUser()

            val criterion = DataBuilder.createExampleUserCriterion()
            val request = getUserCriterionRequest()

            mockCurrentUser(user)
            coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

            val result = service.createCriterion(request)

            assertCriterionEquality(criterion, result)
        }
}
