package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.toFetcherPaper
import se.uulm.snowballr.backend.model.exception.FetcherException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

class SearchFetcherPapersTest : FetcherServiceTest() {
    fun getExampleRequest(project: Project): GrpcProjectPaper.SearchQuery = GrpcProjectPaper.SearchQuery.newBuilder()
        .setProjectId(project.id.toString())
        .build()

    @Test
    fun `When a user searches papers and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(
            fetchers = mapOf(
                "foo" to emptyMap(),
                "bar" to emptyMap(),
            ),
        )
        val projectResult = Result.success(project)
        val fooPaper = DataBuilder.createExamplePaper(externalId = "fooId")
        val fooFetcherPaper = fooPaper.toFetcherPaper()
        val barPaper = DataBuilder.createExamplePaper(externalId = "barId")
        val barFetcherPaper = barPaper.toFetcherPaper()

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { fetcherManagerMock.searchPapers("foo", request.query, any()) } returns setOf(fooFetcherPaper)
        coEvery { fetcherManagerMock.searchPapers("bar", request.query, any()) } returns setOf(barFetcherPaper)
        coEvery { paperRepoMock.getPaperByExternalId("fooId") } returns Result.success(fooPaper)
        coEvery { paperRepoMock.getPaperByExternalId("barId") } returns Result.success(barPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns false
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, barPaper.id) } returns false

        val papers = service.searchFetcherPapers(request).papersList

        assertEquals(2, papers.size)
        assertTrue(papers.any { p -> p.id == fooPaper.id.toString() })
        assertTrue(papers.any { p -> p.id == barPaper.id.toString() })
    }

    @Test
    fun `When a user searches paper, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.success(project)

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery {
            projectAccessCheckerMock.isAllowedToReadProject(user, project.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.searchFetcherPapers(request) }
    }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectResult = Result.failure<Project>(TestSpecificException())

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }

        assertThrows<TestSpecificException> { service.searchFetcherPapers(request) }
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
        val fooPaper = DataBuilder.createExamplePaper(externalId = "fooId")
        val fooFetcherPaper = fooPaper.toFetcherPaper()

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { fetcherManagerMock.searchPapers("foo", request.query, any()) } returns setOf(fooFetcherPaper)
        coEvery {
            fetcherManagerMock.searchPapers("bar", request.query, any())
        } throws FetcherException("Failed to search bar papers")
        coEvery { paperRepoMock.getPaperByExternalId("fooId") } returns Result.success(fooPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns false

        val papers = service.searchFetcherPapers(request).papersList

        assertEquals(1, papers.size)
        assertEquals(papers[0].id, fooPaper.id.toString())
    }

    @Test
    fun `When a fetched paper already exists in the project, then it is not returned`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(fetchers = mapOf("foo" to emptyMap()))
        val projectResult = Result.success(project)
        val fooPaper = DataBuilder.createExamplePaper(externalId = "fooId")
        val fooFetcherPaper = fooPaper.toFetcherPaper()

        val request = getExampleRequest(project)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { fetcherManagerMock.searchPapers("foo", request.query, any()) } returns setOf(fooFetcherPaper)
        coEvery { paperRepoMock.getPaperByExternalId("fooId") } returns Result.success(fooPaper)
        coEvery { projectPaperRepoMock.doesProjectPaperExist(project.id, fooPaper.id) } returns true

        val papers = service.searchFetcherPapers(request).papersList

        assertEquals(0, papers.size)
    }

    @Test
    fun `When retrieving a paper by its external ID fails, then it is returned without checking for existence`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = mapOf("foo" to emptyMap()))
            val projectResult = Result.success(project)
            val fooPaper = DataBuilder.createExamplePaper(externalId = "fooId")
            val fooFetcherPaper = fooPaper.toFetcherPaper()

            val request = getExampleRequest(project)

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { fetcherManagerMock.searchPapers("foo", request.query, any()) } returns setOf(fooFetcherPaper)
            coEvery { paperRepoMock.getPaperByExternalId("fooId") } returns Result.failure(TestSpecificException())

            val papers = service.searchFetcherPapers(request).papersList

            assertEquals(1, papers.size)
            assertEquals(papers[0].title, fooPaper.title)
            assertEquals(papers[0].id, "")
            coVerify(exactly = 0) { projectPaperRepoMock.doesProjectPaperExist(any(), any()) }
        }

    @Test
    fun `When a fetched paper doesn't have an external ID, then it is returned without checking for existence`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(fetchers = mapOf("foo" to emptyMap()))
            val projectResult = Result.success(project)
            val fooPaper = DataBuilder.createExamplePaper(externalId = null)
            val fooFetcherPaper = fooPaper.toFetcherPaper()

            val request = getExampleRequest(project)

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
            coEvery { fetcherManagerMock.searchPapers("foo", request.query, any()) } returns setOf(fooFetcherPaper)

            val papers = service.searchFetcherPapers(request).papersList

            assertEquals(1, papers.size)
            assertEquals(papers[0].title, fooPaper.title)
            assertEquals(papers[0].id, "")
            coVerify(exactly = 0) { paperRepoMock.getPaperByExternalId(any()) }
            coVerify(exactly = 0) { projectPaperRepoMock.doesProjectPaperExist(any(), any()) }
        }
}
