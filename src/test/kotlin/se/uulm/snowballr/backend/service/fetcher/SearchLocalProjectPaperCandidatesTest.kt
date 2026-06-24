package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.Project
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

class SearchLocalProjectPaperCandidatesTest : FetcherServiceTest() {
    fun getExampleRequest(project: Project): GrpcProjectPaper.SearchQuery = GrpcProjectPaper.SearchQuery.newBuilder()
        .setProjectId(project.id.toString())
        .build()

    @Test
    fun `When a user searches papers and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val fooPaper = DataBuilder.createExamplePaper(externalId = "fooId")
        val barPaper = DataBuilder.createExamplePaper(externalId = "barId")

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { paperRepoMock.getPapersBySearchQuery(request.query) } returns listOf(fooPaper, barPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns false
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, barPaper.id) } returns false

        val papers = service.searchLocalProjectPaperCandidates(request).papersList

        assertEquals(2, papers.size)
        assertTrue(papers.any { p -> p.id == fooPaper.id.toString() })
        assertTrue(papers.any { p -> p.id == barPaper.id.toString() })
    }

    @Test
    fun `When a user searches papers, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.searchLocalProjectPaperCandidates(request) }
    }

    @Test
    fun `When a matching paper already exists in the project, then it is not returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val fooPaper = DataBuilder.createExamplePaper(externalId = "fooId")

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { paperRepoMock.getPapersBySearchQuery(request.query) } returns listOf(fooPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns true

        val papers = service.searchLocalProjectPaperCandidates(request).papersList

        assertEquals(0, papers.size)
    }
}
