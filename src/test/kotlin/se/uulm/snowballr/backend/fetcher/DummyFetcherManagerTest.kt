package se.uulm.snowballr.backend.fetcher

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
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
    fun `When searching with an unknown fetcher, then a FetcherNotFoundException is thrown`() = runTest {
        val exception = assertThrows<FetcherNotFoundException> {
            search("snowballing", fetcher = "SemanticScholar.py")
        }

        assertThat(exception.message).contains("Fetcher \"SemanticScholar.py\" not found.")
    }

    @Test
    fun `When fetching forward references with an unknown fetcher, then a FetcherNotFoundException is thrown`() =
        runTest {
            val paper = search("snowballing").first()

            val exception = assertThrows<FetcherNotFoundException> {
                DummyFetcherManager().fetchForwardReferences("SemanticScholar.py", paper, emptyMap())
            }

            assertThat(exception.message).contains("Fetcher \"SemanticScholar.py\" not found.")
        }

    @Test
    fun `When fetching backward references with an unknown fetcher, then a FetcherNotFoundException is thrown`() =
        runTest {
            val paper = search("snowballing").first()

            val exception = assertThrows<FetcherNotFoundException> {
                DummyFetcherManager().fetchBackwardReferences("SemanticScholar.py", paper, emptyMap())
            }

            assertThat(exception.message).contains("Fetcher \"SemanticScholar.py\" not found.")
        }

    @Test
    fun `When searching papers, then every paper has a distinct DOI`() = runTest {
        val papers = search("snowballing")

        assertThat(papers.map { it.doi() }).doesNotHaveDuplicates()
    }
}
