package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.FetcherApi
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.testCoroutine
import snowballr.ProjectOuterClass
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class ProjectTableRepoTest : H2DatabaseTest(arrayOf(ProjectTable), true) {
    private val repo = ProjectTableRepo(db)

    @Nested
    inner class CreateProject {
        @Test
        fun `When a project is created, then the passed values are correctly assigned`() =
            testCoroutine {
                val request =
                    ProjectOuterClass.Project.Create
                        .newBuilder()
                        .setName("Test Project")
                        .build()
                val project = repo.createProject(request, testUserId)

                assertThat(project.name).isEqualTo("Test Project")
                assertThat(project.status).isEqualTo(ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE)
                assertThat(project.currentStage).isEqualTo(0)
                assertThat(project.maxStage).isEqualTo(0)
                // Assert default settings from user
                assertThat(project.similarityThreshold).isEqualTo(0F)
                assertThat(project.snowballingType).isEqualTo(ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH)
                assertThat(project.reviewMaybeAllowed).isTrue()
                assertThat(
                    project.reviewDecisionMatrix,
                ).isEqualTo(ProjectOuterClass.ReviewDecisionMatrix.getDefaultInstance())
                FetcherApi.entries.forEach {
                    assertThat(project.fetcherApis).contains(it)
                }
            }

        @Test
        fun `When two projects are created, then they have different IDs`() =
            testCoroutine {
                val request =
                    ProjectOuterClass.Project.Create
                        .newBuilder()
                        .setName("Test Project")
                        .build()
                val project1 = repo.createProject(request, testUserId)
                val project2 = repo.createProject(request, testUserId)

                assertThat(project1.id).isNotEqualTo(project2.id)
            }

        @Test
        fun `When a project is created, but the assigned user doesn't exist, then an exception is thrown`() =
            testCoroutine {
                val request =
                    ProjectOuterClass.Project.Create
                        .newBuilder()
                        .setName("Test Project")
                        .build()

                assertThrows<NotFoundException.User> { repo.createProject(request, UUID.randomUUID().toString()) }
            }
    }
}
