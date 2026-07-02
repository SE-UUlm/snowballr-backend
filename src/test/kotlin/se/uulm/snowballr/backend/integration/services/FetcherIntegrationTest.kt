package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FetcherIntegrationTest : IntegrationTest() {
    @Nested
    inner class SearchLocalProjectPaperCandidates {
        @Test
        fun `When no papers exist, then the result is empty`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val result = fetcherService.searchLocalProjectPaperCandidates(project.id, "Something about IT")

            assertTrue(result.isEmpty())
        }

        @Test
        fun `When a matching paper is not in the project, then it is returned`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val paper = createPaper("Something about IT")

            val result = fetcherService.searchLocalProjectPaperCandidates(project.id, "Something about IT")

            assertTrue(result.any { it.id == paper.id })
        }

        @Test
        fun `When a matching paper is already in the project, then it is not returned`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val paper = createPaper("Something about IT")
            addToProject(project, paper)

            val result = fetcherService.searchLocalProjectPaperCandidates(project.id, "Something about IT")

            assertFalse(result.any { it.id == paper.id })
        }

        @Test
        fun `When one matching paper is in the project and another is not, then only the non-project paper is returned`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val inProject = createPaper("Something about IT")
                val notInProject = createPaper("Something about AI")
                addToProject(project, inProject)

                val result =
                    fetcherService.searchLocalProjectPaperCandidates(project.id, "Something about")

                assertFalse(result.any { it.id == inProject.id })
                assertTrue(result.any { it.id == notInProject.id })
            }

        @Test
        fun `When a non-member searches for papers, then access is denied`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val outsider = addUser(DataBuilder.createExampleUser(email = "outsider@example.com"))

            actAsUser(outsider.id) {
                assertThrows<UnauthorizedException> {
                    fetcherService.searchLocalProjectPaperCandidates(project.id, "Something about IT")
                }
            }
        }
    }
}
