package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.FetcherApi
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.testCoroutine
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class ProjectTableRepoTest : H2DatabaseTest(arrayOf(ProjectTable), true) {
    private val repo = ProjectTableRepo(db)

    private suspend fun createExampleProject(name: String, status: ProjectStatus): UUID = db.dbQuery {
        ProjectTable
            .insertAndGetId {
                it[ProjectTable.name] = name
                it[ProjectTable.status] = status
                it[currentStage] = 0
                it[maxStage] = 0
                it[similarityThreshold] = 0F
                it[snowballingType] = SnowballingType.SNOWBALLING_TYPE_BOTH
                it[reviewMaybeAllowed] = true
                it[reviewDecisionMatrixBinary] = ReviewDecisionMatrix.getDefaultInstance().toByteArray()
                it[fetcherApis] = FetcherApi.entries.toList()
                it[createdBy] = testUserId
            }.value
    }

    @Nested
    inner class CreateProject {
        @Test
        fun `When a project is created, then the passed values are correctly assigned`() = testCoroutine {
            val request = Project.Create.newBuilder().setName("Test Project").build()
            val project = repo.createProject(request, testUserId)

            assertThat(project.name).isEqualTo("Test Project")
            assertThat(project.status).isEqualTo(ProjectStatus.PROJECT_STATUS_ACTIVE)
            assertThat(project.currentStage).isEqualTo(0)
            assertThat(project.maxStage).isEqualTo(0)
            // Assert default settings from user
            assertThat(project.similarityThreshold).isEqualTo(0F)
            assertThat(project.snowballingType).isEqualTo(SnowballingType.SNOWBALLING_TYPE_BOTH)
            assertThat(project.reviewMaybeAllowed).isTrue()
            assertThat(project.reviewDecisionMatrix).isEqualTo(ReviewDecisionMatrix.getDefaultInstance())
            FetcherApi.entries.forEach {
                assertThat(project.fetcherApis).contains(it)
            }
        }

        @Test
        fun `When two projects are created, then they have different IDs`() = testCoroutine {
            val request = Project.Create.newBuilder().setName("Test Project").build()
            val project1 = repo.createProject(request, testUserId)
            val project2 = repo.createProject(request, testUserId)

            assertThat(project1.id).isNotEqualTo(project2.id)
        }

        @Test
        fun `When a project is created, but the assigned user doesn't exist, then an exception is thrown`() =
            testCoroutine {
                val request = Project.Create.newBuilder().setName("Test Project").build()
                assertThrows<NotFoundException.User> { repo.createProject(request, UUID.randomUUID()) }
            }
    }

    @Nested
    inner class GetAllProjects {
        @Test
        fun `When projects are found, then all projects are returned`() = testCoroutine {
            val project1Id = createExampleProject("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
            val project2Id = createExampleProject("Test Project 2", ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(2)
            val firstProject = projects.find { it.id == project1Id }
            assertThat(firstProject).isNotNull
            val secondProject = projects.find { it.id == project2Id }
            assertThat(secondProject).isNotNull
        }

        @Test
        fun `When archived and deleted projects exist, then they are not returned`() = testCoroutine {
            val project1Id = createExampleProject("Test Project 1", ProjectStatus.PROJECT_STATUS_ACTIVE)
            val project2Id = createExampleProject("Test Project 2", ProjectStatus.PROJECT_STATUS_ARCHIVED)
            val project3Id = createExampleProject("Test Project 3", ProjectStatus.PROJECT_STATUS_DELETED)

            val projects = repo.getAllProjects()
            assertThat(projects).hasSize(1)
            val firstProject = projects.find { it.id == project1Id }
            assertThat(firstProject).isNotNull
            val secondProject = projects.find { it.id == project2Id }
            assertThat(secondProject).isNull()
            val thirdProject = projects.find { it.id == project3Id }
            assertThat(thirdProject).isNull()
        }
    }
}
