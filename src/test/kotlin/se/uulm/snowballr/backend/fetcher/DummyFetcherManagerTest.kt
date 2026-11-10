package se.uulm.snowballr.backend.fetcher

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import kotlin.random.Random

class DummyFetcherManagerTest {
    private fun FetcherPaper.doi() = externalIds.single { it.type == ExternalIdType.DOI }.value

    private suspend fun search(query: String, fetcher: String = "d-1", random: Random = Random(0)) =
        DummyFetcherManager(random).searchPapers(fetcher, query, emptyMap())

    @Test
    fun `When searching papers, then every title contains the trimmed search query`() = runTest {
        val papers = search("  snowballing  ")

        assertThat(papers).isNotEmpty()
        assertThat(papers).allSatisfy { paper ->
            assertThat(paper.title).contains("snowballing")
            assertThat(paper.title).doesNotContain("%s", "  snowballing")
        }
    }

    @Test
    fun `When searching with an unknown fetcher, then no papers are returned`() = runTest {
        val papers = search("snowballing", fetcher = "SemanticScholar.py")

        assertThat(papers).isEmpty()
    }

    @Test
    fun `When searching papers, then every paper has a distinct DOI`() = runTest {
        val papers = search("snowballing")

        assertThat(papers.map { it.doi() }).doesNotHaveDuplicates()
    }
}
