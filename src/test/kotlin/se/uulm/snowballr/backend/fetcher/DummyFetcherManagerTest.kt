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
    fun `When searching papers, then the trimmed query is matched case-insensitively against the title`() = runTest {
        val papers = search("  gene EDITING  ")

        assertThat(papers.map { it.title }).containsExactly("The Ethics of Gene Editing")
    }

    @Test
    fun `When searching papers by DOI, then the paper with that DOI is returned`() = runTest {
        val papers = search("10.1111/abc.1234")

        assertThat(papers.map { it.title }).containsExactly("Advances in Deep Learning")
    }

    @Test
    fun `When searching papers by author name, then papers of that author are returned`() = runTest {
        val papers = search("Henry Garcia")

        assertThat(papers.map { it.title }).containsExactlyInAnyOrder(
            "Exploring the Universe of AI",
            "Augmented Reality: Shaping the Future of Education",
            "The Evolution of E-Commerce in the Digital Age",
        )
    }

    @Test
    fun `When searching papers with a query matching no paper, then no papers are returned`() = runTest {
        val papers = search("snowballing")

        assertThat(papers).isEmpty()
    }

    @Test
    fun `When searching with an unknown fetcher, then a FetcherNotFoundException is thrown`() = runTest {
        val exception = assertThrows<FetcherNotFoundException> {
            search("deep learning", fetcher = "SemanticScholar.py")
        }

        assertThat(exception.message).contains("Fetcher \"SemanticScholar.py\" not found.")
    }

    @Test
    fun `When fetching forward references with an unknown fetcher, then a FetcherNotFoundException is thrown`() =
        runTest {
            val paper = search("deep learning").first()

            val exception = assertThrows<FetcherNotFoundException> {
                DummyFetcherManager().fetchForwardReferences("SemanticScholar.py", paper, emptyMap())
            }

            assertThat(exception.message).contains("Fetcher \"SemanticScholar.py\" not found.")
        }

    @Test
    fun `When fetching backward references with an unknown fetcher, then a FetcherNotFoundException is thrown`() =
        runTest {
            val paper = search("deep learning").first()

            val exception = assertThrows<FetcherNotFoundException> {
                DummyFetcherManager().fetchBackwardReferences("SemanticScholar.py", paper, emptyMap())
            }

            assertThat(exception.message).contains("Fetcher \"SemanticScholar.py\" not found.")
        }

    @Test
    fun `When searching papers, then every paper has a distinct DOI`() = runTest {
        val papers = search("paper")

        assertThat(papers).hasSize(12)
        assertThat(papers.map { it.doi() }).doesNotHaveDuplicates()
    }

    @Test
    fun `When searching papers, then every paper carries the fetcher metadata`() = runTest {
        val papers = search("paper", fetcher = "d-2")

        assertThat(papers).isNotEmpty()
        assertThat(papers).allSatisfy { paper ->
            assertThat(paper.fetcherMetadata).containsEntry("fetcherId", "d-2")
        }
    }
}
