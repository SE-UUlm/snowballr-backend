package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.ProjectPaperNotFoundException
import se.uulm.snowballr.backend.repository.PaperTableRepo
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertProjectPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.Project
import java.sql.SQLException
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectPaperTableRepoTest : RepositoryTest(arrayOf(ProjectPaperTable, ProjectTable, PaperTable), true) {
    private val repo = ProjectPaperTableRepo(db)
    private val paperRepo = PaperTableRepo(db)

    @Nested
    inner class GetProjectPaperById {
        @Test
        fun `When a project paper is found, then a successful result with the correct project paper is returned`() =
            runTest {
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                val projectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)

                val result = repo.getProjectPaperById(projectPaperId)

                val projectPaper = assertResultSuccess(result)
                assertThat(projectPaper.id).isEqualTo(projectPaperId)
                assertThat(projectPaper.projectId).isEqualTo(projectId)
                assertThat(projectPaper.paperId).isEqualTo(paperId)
                assertThat(projectPaper.localPaperId).isEqualTo(0)
                assertThat(projectPaper.stage).isEqualTo(0)
                assertThat(projectPaper.decision).isEqualTo(ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED)
                assertThat(projectPaper.createdBy).isEqualTo(testUserId)
            }

        @Test
        fun `When a project paper is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getProjectPaperById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class GetProjectPaperByRelativeId {
        @Test
        fun `When no project with the given id exists, then a NotFoundException is thrown`() = runTest {
            val projectId = insertProjectAndGetId(createdBy = testUserId)
            val paperId = insertPaperAndGetId()
            val projectPaperId =
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
            val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

            assertThrows<ProjectPaperNotFoundException> {
                repo.getProjectPaperByRelativeId(UUID.randomUUID(), projectPaper.localPaperId)
            }
        }

        @Test
        fun `When no project paper with the given local id exists in the project, then a NotFoundException is thrown`() =
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
                var projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()
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
    inner class DoesProjectPaperExistsByPaperId {
        @Test
        fun `When a project paper exists that is associated with the given paper ID in the project identified by the given project ID, then true is returned`() =
            runTest {
                val projectId =
                    insertProjectAndGetId(createdBy = testUserId)
                val paperId = insertPaperAndGetId()
                insertProjectPaperAndGetId(paperId = paperId, projectId = projectId, createdBy = testUserId)
                val isProjectPaperExisting = repo.doesProjectPaperExist(projectId, paperId)

                assertTrue(isProjectPaperExisting)
            }

        @Test
        fun `When no project paper exists that is associated with the given paper ID in the project identified by the given project ID, then false is returned`() =
            runTest {
                val projectId = UUID.randomUUID()
                val paperId = UUID.randomUUID()
                val isProjectExisting = repo.doesProjectPaperExist(projectId, paperId)

                assertFalse(isProjectExisting)
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
                val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()

                val paper = paperRepo.getPaperById(paperId).getOrThrow()
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
                val projectPaper = repo.getProjectPaperById(projectPaperId).getOrThrow()
                val nonProjectPaperId =
                    insertProjectPaperAndGetId(paperId = paperId, projectId = projectId2, createdBy = testUserId)

                val paper = paperRepo.getPaperById(paperId).getOrThrow()
                val projectPapers = repo.getAllProjectPapersWithPapers(projectId1)

                assertThat(projectPapers).hasSize(1)
                assertThat(projectPapers).anyMatch { it.projectPaper == projectPaper }
                assertThat(projectPapers).anyMatch { it.paper == paper }
                assertThat(projectPapers).noneMatch { it.projectPaper.id == nonProjectPaperId }
            }
    }

    @Nested
    inner class AddPaperToProject {
        @Test
        fun `When a project paper is added to a project, but the assigned user doesn't exist, then a SQLException is thrown`() =
            runTest {
                val request = Project.Paper.Add
                    .newBuilder()
                    .setPaperId(UUID.randomUUID().toString())
                    .setProjectId(UUID.randomUUID().toString())
                    .build()
                assertThrows<SQLException> {
                    repo.addPaperToProject(request, UUID.randomUUID())
                }
            }

        @Test
        fun `When a project paper is added to a project with the correct values, then the project paper is returned`() =
            runTest {
                val paperId = insertPaperAndGetId()
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val request = Project.Paper.Add.newBuilder()
                    .setPaperId(paperId.toString())
                    .setProjectId(projectId.toString())
                    .setStage(0)
                    .build()

                val projectPaper = assertDoesNotThrow { repo.addPaperToProject(request, testUserId) }
                assertEquals(paperId, projectPaper.paperId)
                assertEquals(projectId, projectPaper.projectId)
                assertEquals(0, projectPaper.localPaperId)
                assertEquals(0, projectPaper.stage)
                assertEquals(ProjectOuterClass.PaperDecision.PAPER_DECISION_UNSPECIFIED, projectPaper.decision)
                assertEquals(testUserId, projectPaper.createdBy)
            }

        @Test
        fun `When a second project paper is added to the same project with the correct values, then the local paper ID is increased correctly`() =
            runTest {
                val paperId1 = insertPaperAndGetId()
                val projectId = insertProjectAndGetId(createdBy = testUserId)
                val request1 = Project.Paper.Add.newBuilder()
                    .setPaperId(paperId1.toString())
                    .setProjectId(projectId.toString())
                    .setStage(0)
                    .build()

                val paperId2 = insertPaperAndGetId()
                val request2 = Project.Paper.Add.newBuilder()
                    .setPaperId(paperId2.toString())
                    .setProjectId(projectId.toString())
                    .setStage(0)
                    .build()

                assertDoesNotThrow { repo.addPaperToProject(request1, testUserId) }
                val projectPaper2 = assertDoesNotThrow { repo.addPaperToProject(request2, testUserId) }
                assertEquals(1, projectPaper2.localPaperId)
            }

        @Test
        fun `When a second project paper is added to another project with the correct values, then the local paper IDs are independent`() =
            runTest {
                val paperId1 = insertPaperAndGetId()
                val projectId1 = insertProjectAndGetId(createdBy = testUserId)
                val request1 = Project.Paper.Add.newBuilder()
                    .setPaperId(paperId1.toString())
                    .setProjectId(projectId1.toString())
                    .setStage(0)
                    .build()

                val paperId2 = insertPaperAndGetId()
                val projectId2 = insertProjectAndGetId(createdBy = testUserId)
                val request2 = Project.Paper.Add.newBuilder()
                    .setPaperId(paperId2.toString())
                    .setProjectId(projectId2.toString())
                    .setStage(0)
                    .build()

                val projectPaper1 = assertDoesNotThrow { repo.addPaperToProject(request1, testUserId) }
                val projectPaper2 = assertDoesNotThrow { repo.addPaperToProject(request2, testUserId) }
                assertEquals(0, projectPaper1.localPaperId)
                assertEquals(0, projectPaper2.localPaperId)
            }
    }
}
