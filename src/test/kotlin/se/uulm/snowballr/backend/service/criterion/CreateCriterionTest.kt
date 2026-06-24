package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

class CreateCriterionTest : CriterionServiceTest() {
    private fun getProjectCriterionRequest(projectId: String): GrpcCriterion.Create {
        return GrpcCriterion.Create.newBuilder()
            .setTag("Tag")
            .setName("Criterion")
            .setDescription("Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            .setProjectId(projectId)
            .build()
    }

    private fun getUserCriterionRequest(): GrpcCriterion.Create {
        return GrpcCriterion.Create.newBuilder()
            .setTag("Tag")
            .setName("Criterion")
            .setDescription("Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            .build()
    }

    @Test
    fun `When a user creates a project criterion and has access, then the created criterion has the correct values`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
            val request = getProjectCriterionRequest(project.id.toString())

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

            val request = getProjectCriterionRequest(project.id.toString())

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
