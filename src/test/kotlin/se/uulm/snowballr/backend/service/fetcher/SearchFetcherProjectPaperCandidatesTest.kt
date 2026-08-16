package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.exception.FetcherException

class SearchFetcherProjectPaperCandidatesTest : FetcherServiceTest() {
    val exampleQuery = "exampleQuery"

    @Test
    fun `When a user searches papers and has access, then the correct values are returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(
            fetchers = mapOf(
                "foo" to emptyMap(),
                "bar" to emptyMap(),
            ),
        )
        val projectResult = Result.success(project)
        val fooExternalId = DataBuilder.createExampleExternalId(value = "fooId")
        val fooPaper = DataBuilder.createExamplePaper(externalIds = listOf(fooExternalId))
        val fooFetcherPaper = fooPaper.toFetcherPaper()
        val barExternalId = DataBuilder.createExampleExternalId(value = "barId")
        val barPaper = DataBuilder.createExamplePaper(externalIds = listOf(barExternalId))
        val barFetcherPaper = barPaper.toFetcherPaper()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { fetcherManagerMock.searchPapers("foo", exampleQuery, any()) } returns setOf(fooFetcherPaper)
        coEvery { fetcherManagerMock.searchPapers("bar", exampleQuery, any()) } returns setOf(barFetcherPaper)
        coEvery {
            paperRepoMock.getPapersByExternalIds(listOf(fooExternalId, barExternalId))
        } returns listOf(fooPaper, barPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns false
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, barPaper.id) } returns false

        val papers = service.searchFetcherProjectPaperCandidates(project.id, exampleQuery)

        assertEquals(2, papers.size)
        assertTrue(papers.any { p -> p.id == fooPaper.id })
        assertTrue(papers.any { p -> p.id == barPaper.id })
    }

    @Test
    fun `When a user searches papers, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.searchFetcherProjectPaperCandidates(project.id, exampleQuery) }
    }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.failure<Project>(TestSpecificException())

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult

        assertThrows<TestSpecificException> { service.searchFetcherProjectPaperCandidates(project.id, exampleQuery) }
    }

    @Test
    fun `When searching for one fetcher fails, then another fetcher is not affected`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(
            fetchers = mapOf(
                "foo" to emptyMap(),
                "bar" to emptyMap(),
            ),
        )
        val projectResult = Result.success(project)
        val fooExternalId = DataBuilder.createExampleExternalId(value = "fooId")
        val fooPaper = DataBuilder.createExamplePaper(externalIds = listOf(fooExternalId))
        val fooFetcherPaper = fooPaper.toFetcherPaper()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { fetcherManagerMock.searchPapers("foo", exampleQuery, any()) } returns setOf(fooFetcherPaper)
        coEvery {
            fetcherManagerMock.searchPapers("bar", exampleQuery, any())
        } throws FetcherException("Failed to search bar papers")
        coEvery { paperRepoMock.getPapersByExternalIds(listOf(fooExternalId)) } returns listOf(fooPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns false

        val papers = service.searchFetcherProjectPaperCandidates(project.id, exampleQuery)

        assertEquals(1, papers.size)
        assertEquals(fooPaper.id, papers[0].id)
    }

    @Test
    fun `When a fetched paper already exists in the project, then it is not returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(fetchers = mapOf("foo" to emptyMap()))
        val projectResult = Result.success(project)
        val fooExternalId = DataBuilder.createExampleExternalId(value = "fooId")
        val fooPaper = DataBuilder.createExamplePaper(externalIds = listOf(fooExternalId))
        val fooFetcherPaper = fooPaper.toFetcherPaper()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { fetcherManagerMock.searchPapers("foo", exampleQuery, any()) } returns setOf(fooFetcherPaper)
        coEvery { paperRepoMock.getPapersByExternalIds(listOf(fooExternalId)) } returns listOf(fooPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns true

        val papers = service.searchFetcherProjectPaperCandidates(project.id, exampleQuery)

        assertEquals(0, papers.size)
    }

    @Test
    fun `When paper is not found by its external IDs, then it is returned without checking for existence`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(fetchers = mapOf("foo" to emptyMap()))
        val projectResult = Result.success(project)
        val fooExternalId = DataBuilder.createExampleExternalId(value = "fooId")
        val fooPaper = DataBuilder.createExamplePaper(externalIds = listOf(fooExternalId))
        val fooFetcherPaper = fooPaper.toFetcherPaper()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery { fetcherManagerMock.searchPapers("foo", exampleQuery, any()) } returns setOf(fooFetcherPaper)
        coEvery { paperRepoMock.getPapersByExternalIds(listOf(fooExternalId)) } returns emptyList()

        val papers = service.searchFetcherProjectPaperCandidates(project.id, exampleQuery)

        assertEquals(1, papers.size)
        assertEquals(fooPaper.title, papers[0].title)
        assertEquals(null, papers[0].id)
        coVerify(exactly = 0) { projectPaperRepoMock.doesProjectPaperExist(any(), any()) }
    }

    @Test
    fun `When a fetched paper doesn't have any external IDs, then it is returned without checking for existence`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = mapOf("foo" to emptyMap()))
            val projectResult = Result.success(project)
            val fooPaper = DataBuilder.createExamplePaper(externalIds = emptyList())
            val fooFetcherPaper = fooPaper.toFetcherPaper()

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
            coEvery { fetcherManagerMock.searchPapers("foo", exampleQuery, any()) } returns setOf(fooFetcherPaper)
            coEvery { paperRepoMock.getPapersByExternalIds(emptyList()) } returns emptyList()

            val papers = service.searchFetcherProjectPaperCandidates(project.id, exampleQuery)

            assertEquals(1, papers.size)
            assertEquals(fooPaper.title, papers[0].title)
            assertEquals(null, papers[0].id)
            coVerify(exactly = 0) { projectPaperRepoMock.doesProjectPaperExist(any(), any()) }
        }
}
