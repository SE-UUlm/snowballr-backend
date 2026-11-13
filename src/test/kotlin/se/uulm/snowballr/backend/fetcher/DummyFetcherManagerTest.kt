package se.uulm.snowballr.backend.fetcher

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.paper.normalized
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import kotlin.random.Random

class DummyFetcherManagerTest {
    @Test
    fun `When searching papers, then the trimmed query is matched case-insensitively against the title`() = runTest {
        val papers = search("  gene EDITING  ")

        assertThat(papers.map { it.title }).containsExactly("The Ethics of Gene Editing")
    }

    @Test
    fun `When using an unknown fetcher, then every method throws a FetcherNotFoundException`() = runTest {
        val paper = paperWithDoi("10.1111/abc.1234")

        val exceptions = listOf(
            assertThrows<FetcherNotFoundException> { search("deep learning", fetcher = "unknown-fetcher") },
            assertThrows<FetcherNotFoundException> { forwardReferences(paper, fetcher = "unknown-fetcher") },
            assertThrows<FetcherNotFoundException> { backwardReferences(paper, fetcher = "unknown-fetcher") },
        )

        assertThat(exceptions).allSatisfy { exception ->
            assertThat(exception.message).contains("Fetcher \"unknown-fetcher\" not found.")
        }
    }

    @Test
    fun `When fetching backward references, then the papers cited by the paper are returned`() = runTest {
        val paper = paperWithDoi("10.1111/abc.1234") // Advances in Deep Learning

        val references = backwardReferences(paper)

        assertThat(references.map { it.title }).containsExactlyInAnyOrder(
            "Exploring the Universe of AI",
            "Artificial Intelligence in Healthcare",
            "Neuroscience and the Human Brain",
        )
    }

    @Test
    fun `When fetching forward references, then the papers citing the paper are returned`() = runTest {
        val paper = paperWithDoi("10.1111/abc.1234") // Advances in Deep Learning

        val references = forwardReferences(paper)

        assertThat(references.map { it.title }).containsExactlyInAnyOrder(
            "The Evolution of E-Commerce in the Digital Age",
            "The Rise of Virtual Reality in Entertainment",
        )
    }

    @Test
    fun `When searching with paper query, then return all papers`() = runTest {
        val papers = search("paper")

        assertThat(papers).hasSize(12)
        assertThat(papers.map { it.doi() }).doesNotHaveDuplicates()
    }

    private suspend fun search(query: String, fetcher: String = "d-1", random: Random = Random(0)) =
        DummyFetcherManager(random).searchPapers(fetcher, query, emptyMap())

    private suspend fun forwardReferences(paper: FetcherPaper, fetcher: String = "d-1") =
        DummyFetcherManager(Random(0)).fetchForwardReferences(fetcher, paper, emptyMap())

    private suspend fun backwardReferences(paper: FetcherPaper, fetcher: String = "d-1") =
        DummyFetcherManager(Random(0)).fetchBackwardReferences(fetcher, paper, emptyMap())

    private fun FetcherPaper.doi(): String? =
        externalIds.firstOrNull { it.type == ExternalIdType.DOI }?.normalized()?.value

    private suspend fun paperWithDoi(doi: String) = search(doi).single()

}
