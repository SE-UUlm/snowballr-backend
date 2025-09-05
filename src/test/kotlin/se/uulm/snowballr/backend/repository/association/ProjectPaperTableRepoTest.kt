package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.ProjectPaperNotFoundException
import se.uulm.snowballr.backend.repository.PaperTableRepo
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import snowballr.ProjectOuterClass
import java.util.UUID
import kotlin.random.Random

class ProjectPaperTableRepoTest : RepositoryTest(arrayOf(ProjectPaperTable, ProjectTable, PaperTable), true) {
    private val repo = ProjectPaperTableRepo(db)
    private val paperRepo = PaperTableRepo(db)

    @Nested
    inner class GetProjectPaperById {
        @Test
        fun `When a project paper is found, then the correct project paper is returned`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val projectPaper = repo.getProjectPaperById(projectPaperId)

            assertThat(projectPaper.id).isEqualTo(projectPaperId)
            assertThat(projectPaper.projectId).isEqualTo(projectId)
            assertThat(projectPaper.paperId).isEqualTo(paperId)
            assertThat(projectPaper.localPaperId).isEqualTo(0)
            assertThat(projectPaper.stage).isEqualTo(0)
            assertThat(projectPaper.decision).isEqualTo(ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED)
            assertThat(projectPaper.createdBy).isEqualTo(testUserId)
        }

        @Test
        fun `When a project paper is not found, then an exception is thrown`() = runTest {
            assertThrows<SnowballRException.NotFoundException> { repo.getProjectPaperById(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class GetProjectPaperByRelativeId {
        @Test
        fun `When no project with the given id exists, then a not found exception is thrown`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val projectPaper = repo.getProjectPaperById(projectPaperId)

            assertThrows<ProjectPaperNotFoundException> {
                repo.getProjectPaperByRelativeId(UUID.randomUUID(), projectPaper.localPaperId)
            }
        }

        @Test
        fun `When no project paper with the given local id exists in the project, then a not found exception is thrown`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)

                assertThrows<ProjectPaperNotFoundException> {
                    repo.getProjectPaperByRelativeId(
                        projectId,
                        Random.nextLong(),
                    )
                }
            }

        @Test
        fun `When a project paper with the given local id in the project is found, then the correct project paper is returned`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                val projectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
                var projectPaper = repo.getProjectPaperById(projectPaperId)
                projectPaper = assertDoesNotThrow {
                    repo.getProjectPaperByRelativeId(projectId, projectPaper.localPaperId)
                }

                assertThat(projectPaper.id).isEqualTo(projectPaperId)
                assertThat(projectPaper.projectId).isEqualTo(projectId)
                assertThat(projectPaper.paperId).isEqualTo(paperId)
                assertThat(projectPaper.localPaperId).isEqualTo(0)
                assertThat(projectPaper.stage).isEqualTo(0)
                assertThat(projectPaper.decision).isEqualTo(ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED)
                assertThat(projectPaper.createdBy).isEqualTo(testUserId)
            }
    }

    @Nested
    inner class GetProjectMembersWithUsers {
        @Test
        fun `When a project, a project paper and the corresponding paper exists, then the project paper with paper is correctly returned`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                val projectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
                val projectPaper = repo.getProjectPaperById(projectPaperId)

                val paper = paperRepo.getPaperById(paperId)
                val projectPapers = repo.getAllProjectPapersWithPapers(projectId)

                assertThat(projectPapers).hasSize(1)
                assertThat(projectPapers).anyMatch { it.projectPaper == projectPaper }
                assertThat(projectPapers).anyMatch { it.paper == paper }
            }

        @Test
        fun `When not all project papers are papers of the selected project, then only the correct project papers with papers are returned`() =
            runTest {
                val projectId1 = insertProjectAndGetId(createdBy = testUserId)
                val projectId2 = insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                val projectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId1, createdBy = testUserId)
                val projectPaper = repo.getProjectPaperById(projectPaperId)
                val nonProjectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId2, createdBy = testUserId)

                val paper = paperRepo.getPaperById(paperId)
                val projectPapers = repo.getAllProjectPapersWithPapers(projectId1)

                assertThat(projectPapers).hasSize(1)
                assertThat(projectPapers).anyMatch { it.projectPaper == projectPaper }
                assertThat(projectPapers).anyMatch { it.paper == paper }
                assertThat(projectPapers).noneMatch { it.projectPaper.id == nonProjectPaperId }
            }
    }
}
