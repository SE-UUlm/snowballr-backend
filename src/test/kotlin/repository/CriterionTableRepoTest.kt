package se.uulm.snowballr.backend.repository

import io.grpc.StatusException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.testCoroutine
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class CriterionTableRepoTest : H2DatabaseTest(arrayOf(CriterionTable, ProjectTable)) {
    private val repo = CriterionTableRepo(db)
    private val projectRepo = ProjectTableRepo(db)

    private suspend fun createExampleProject(): ProjectOuterClass.Project {
        val request =
            ProjectOuterClass.Project.Create
                .newBuilder()
                .build()
        return projectRepo.createProject(request)
    }

    @Nested
    inner class CreateCriterion {
        @GrpcEnumSourceTest(CriterionOuterClass.CriterionCategory::class)
        fun `When a criterion is created, then the values are correctly assigned`(
            category: CriterionOuterClass.CriterionCategory,
        ) = testCoroutine {
            val project = createExampleProject()

            val request =
                CriterionOuterClass.Criterion.Create
                    .newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(category)
                    .setProjectId(project.id)
                    .build()
            val criterion = repo.createCriterion(request)

            Assertions.assertThat(criterion.tag).isEqualTo("Test Tag")
            Assertions.assertThat(criterion.name).isEqualTo("Test Criterion")
            Assertions.assertThat(criterion.description).isEqualTo("Test Description")
            Assertions.assertThat(criterion.category).isEqualTo(category)
        }

        @GrpcEnumSourceTest(CriterionOuterClass.CriterionCategory::class)
        fun `When a criterion is created, but the assigned project doesn't exist, then an exception is thrown`(
            category: CriterionOuterClass.CriterionCategory,
        ) = testCoroutine {
            val request =
                CriterionOuterClass.Criterion.Create
                    .newBuilder()
                    .setTag("Test Tag")
                    .setName("Test Criterion")
                    .setDescription("Test Description")
                    .setCategory(category)
                    .setProjectId("0")
                    .build()

            assertThrows<StatusException> { repo.createCriterion(request) }
        }

        @Test
        fun `When two criteria are created, then they have different IDs`() =
            testCoroutine {
                val project = createExampleProject()

                val request =
                    CriterionOuterClass.Criterion.Create
                        .newBuilder()
                        .setTag("Test Tag")
                        .setName("Test Criterion")
                        .setDescription("Test Description")
                        .setCategory(CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
                        .setProjectId(project.id)
                        .build()
                val criterion1 = repo.createCriterion(request)
                val criterion2 = repo.createCriterion(request)

                Assertions.assertThat(criterion1.id).isNotEqualTo(criterion2.id)
            }
    }
}
