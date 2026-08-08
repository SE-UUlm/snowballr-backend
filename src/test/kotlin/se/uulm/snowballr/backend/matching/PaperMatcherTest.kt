package se.uulm.snowballr.backend.matching

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper

private const val DELTA = 1e-9

class PaperMatcherTest {
    private val defaultConfig = PaperMatchingConfig(1, 0.6, 0.3, 0.1)
    private val matcher = PaperMatcher(defaultConfig)
    private val titleOnlyMatcher = PaperMatcher(PaperMatchingConfig(1, 1.0, 0.0, 0.0))

    /**
     * Creates two strings with a target similarity in [0, 100].
     */
    private fun getSimilarStrings(similarity: Int): Pair<String, String> {
        require(similarity in 1..100)

        val a = "a".repeat(100)
        val b = "b".repeat(100 - similarity) + "a".repeat(similarity)

        return a to b
    }

    @Nested
    inner class Similarity {
        @Test
        fun `When both papers share an External ID, then similarity is 1`() {
            val externalIds = listOf(ExternalId(ExternalIdType.DOI, "doi:10.1/abc"))
            val a = DataBuilder.createExampleFetcherPaper(externalIds = externalIds)
            val b = DataBuilder.createExampleFetcherPaper(externalIds = externalIds)
            assertEquals(1.0, matcher.similarity(a, b), DELTA)
        }

        @Test
        fun `When both papers share an External ID, but also have others, then similarity is 1`() {
            val sameExternalId = ExternalId(ExternalIdType.DOI, "doi:10.1/abc")
            val externalId1 = ExternalId(ExternalIdType.SEMANTIC_SCHOLAR, "12345")
            val externalId2 = ExternalId(ExternalIdType.URL, "https://example.com")
            val a = DataBuilder.createExampleFetcherPaper(externalIds = listOf(sameExternalId, externalId1))
            val b = DataBuilder.createExampleFetcherPaper(externalIds = listOf(externalId2, sameExternalId))
            assertEquals(1.0, matcher.similarity(a, b), DELTA)
        }

        @Test
        fun `When years differ by more than the tolerance, then similarity is 0`() {
            val a = DataBuilder.createExampleFetcherPaper(year = 2000)
            val b = DataBuilder.createExampleFetcherPaper(year = 2002)
            assertEquals(0.0, matcher.similarity(a, b), DELTA)
        }

        @Test
        fun `When years differ by exactly the tolerance, then similarity is positive`() {
            val a = DataBuilder.createExampleFetcherPaper(year = 2000)
            val b = DataBuilder.createExampleFetcherPaper(year = 2001)
            assertTrue(matcher.similarity(a, b) > 0.0)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "' ',John Doe",
                "John Doe,' '",
                "' ',' '",
            ],
        )
        fun `When at least one author list is empty, then authors component is dropped`(
            authorA: String,
            authorB: String,
        ) {
            val authorsA = authorA.split(';').filter { it.isNotBlank() }.map { Author(it, "") }
            val authorsB = authorB.split(';').filter { it.isNotBlank() }.map { Author("", it) }
            val a1 = DataBuilder.createExampleFetcherPaper(
                title = "Some random title",
                authors = authorsA,
                abstract = "Some random abstract",
            )
            val b1 = DataBuilder.createExampleFetcherPaper(
                title = a1.title.reversed(),
                authors = authorsB,
                abstract = a1.abstract.reversed(),
            )
            val emptyResult = matcher.similarity(a1, b1)

            val altMatcher = PaperMatcher(defaultConfig.copy(authorsWeight = 0.0))
            val a2 = a1.copy(authors = listOf(Author("John", "Doe")))
            val b2 = b1.copy(authors = listOf(Author("John", "Doe")))
            val noWeightResult = altMatcher.similarity(a2, b2)

            // Dropping authors is the same as not weighing their similarity
            assertEquals(noWeightResult, emptyResult, DELTA)
        }

        @Test
        fun `When one abstract is blank, then abstract component is dropped`() {
            val authors1 = listOf(Author("John", "Doe"), Author("Jane", "Doe"))
            val authors2 = authors1.map { Author(it.lastName.reversed(), it.firstName.reversed()) }
            val a1 = DataBuilder.createExampleFetcherPaper(
                title = "Some random title",
                authors = authors1,
                abstract = "Some random abstract",
            )
            val b1 = a1.copy(title = a1.title.reversed(), abstract = "", authors = authors2)
            val emptyResult = matcher.similarity(a1, b1)

            val altMatcher = PaperMatcher(defaultConfig.copy(abstractWeight = 0.0))
            val a2 = a1.copy(abstract = "Paper Abstract")
            val b2 = b1.copy(abstract = "Paper Abstract")
            val noWeightResult = altMatcher.similarity(a2, b2)

            // Dropping the abstract is the same as not weighing its similarity
            assertEquals(noWeightResult, emptyResult, DELTA)
        }

        @Test
        fun `When both abstracts are blank, then only title and authors contribute`() {
            val authors1 = listOf(Author("John", "Doe"), Author("Jane", "Doe"))
            val authors2 = authors1.map { Author(it.lastName.reversed(), it.firstName.reversed()) }
            val a1 = DataBuilder.createExampleFetcherPaper(
                title = "Some random title",
                authors = authors1,
                abstract = "",
            )
            val b1 = a1.copy(title = a1.title.reversed(), authors = authors2)
            val emptyResult = matcher.similarity(a1, b1)

            val altMatcher = PaperMatcher(defaultConfig.copy(abstractWeight = 0.0))
            val a2 = a1.copy(abstract = "Paper Abstract")
            val b2 = b1.copy(abstract = "Paper Abstract")
            val noWeightResult = altMatcher.similarity(a2, b2)

            // Dropping the abstract is the same as not weighing its similarity
            assertEquals(noWeightResult, emptyResult, DELTA)
            assertTrue(emptyResult > 0 && noWeightResult > 0)
        }

        @Test
        fun `When titles are identical, then title score is 1`() {
            val a = DataBuilder.createExampleFetcherPaper(title = "Deep Learning for NLP")
            val b = DataBuilder.createExampleFetcherPaper(title = "Deep Learning for NLP")
            assertEquals(1.0, matcher.similarity(a, b), DELTA)
        }

        @Test
        fun `When authors have blank names, then they do not contribute to the authors score`() {
            val blankAuthors = listOf(Author("", ""))
            val withBlankAuthors1 = DataBuilder.createExampleFetcherPaper(
                title = "Quantum Computing",
                abstract = "",
                authors = blankAuthors,
            )
            val withBlankAuthors2 = DataBuilder.createExampleFetcherPaper(
                title = "Medieval History",
                abstract = "",
                authors = blankAuthors,
            )

            val blankResult = matcher.similarity(withBlankAuthors1, withBlankAuthors2)
            val emptyResult = matcher.similarity(
                withBlankAuthors1.copy(authors = emptyList()),
                withBlankAuthors2.copy(authors = emptyList()),
            )

            assertEquals(emptyResult, blankResult, DELTA)
        }

        @Test
        fun `When both papers carry a blank external ID value, then it is not treated as an identity`() {
            val blankExternalId = listOf(ExternalId(ExternalIdType.DOI, ""))
            val a = DataBuilder.createExampleFetcherPaper(
                title = "Quantum Computing",
                abstract = "",
                authors = emptyList(),
                externalIds = blankExternalId,
                year = 1990,
            )
            val b = DataBuilder.createExampleFetcherPaper(
                title = "Medieval History",
                abstract = "",
                authors = emptyList(),
                externalIds = blankExternalId,
                year = 2020,
            )

            val blankResult = matcher.similarity(a, b)
            val emptyResult = matcher.similarity(
                a.copy(externalIds = emptyList()),
                b.copy(externalIds = emptyList()),
            )

            assertEquals(emptyResult, blankResult, DELTA)
        }

        @Test
        fun `When both papers have a conflicting external ID, then the similarity is 0`() {
            val externalIdsA = listOf(
                ExternalId(ExternalIdType.MAG, "123456"),
                ExternalId(ExternalIdType.DOI, "10.1234/abcd"),
            )
            val externalIdsB = listOf(
                ExternalId(ExternalIdType.SEMANTIC_SCHOLAR, "123456"),
                ExternalId(ExternalIdType.DOI, "10.4321/dcba"),
            )
            val a = DataBuilder.createExampleFetcherPaper(externalIds = externalIdsA)
            val b = DataBuilder.createExampleFetcherPaper(externalIds = externalIdsB)

            assertEquals(0.0, matcher.similarity(a, b), DELTA)
        }

        @Test
        fun `When both papers have a conflicting URL, then the similarity is determined by the paper components`() {
            val externalIdsA = listOf(
                ExternalId(ExternalIdType.MAG, "123456"),
                ExternalId(ExternalIdType.URL, "https://example.org"),
            )
            val externalIdsB = listOf(
                ExternalId(ExternalIdType.SEMANTIC_SCHOLAR, "123456"),
                ExternalId(ExternalIdType.URL, "https://redirect-to-example.org"),
            )
            val a = DataBuilder.createExampleFetcherPaper(externalIds = externalIdsA)
            val b = DataBuilder.createExampleFetcherPaper(externalIds = externalIdsB)

            val urlResult = matcher.similarity(a, b)
            val noIdsResult = matcher.similarity(
                a.copy(externalIds = emptyList()),
                b.copy(externalIds = emptyList()),
            )

            assertEquals(noIdsResult, urlResult, DELTA)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "' ',Example Title",
                "Example Title,' '",
                "' ',' '",
            ],
        )
        fun `When at least one paper has a blank title, then title component is dropped`(
            titleA: String,
            titleB: String,
        ) {
            val authors = listOf(Author("John", "Doe"))
            val a1 = DataBuilder.createExampleFetcherPaper(
                title = titleA,
                authors = authors,
                abstract = "Some random abstract",
            )
            val b1 = a1.copy(title = titleB, abstract = a1.abstract.reversed())
            val emptyResult = matcher.similarity(a1, b1)

            val altMatcher = PaperMatcher(defaultConfig.copy(titleWeight = 0.0))
            val a2 = a1.copy(title = "Some random title")
            val b2 = b1.copy(title = "Title random some")
            val noWeightResult = altMatcher.similarity(a2, b2)

            // Dropping title is the same as not weighing its similarity
            assertEquals(noWeightResult, emptyResult, DELTA)
        }
    }

    @Nested
    inner class DeduplicatePapers {
        @Test
        fun `When input is empty, then result is empty`() {
            val result = matcher.deduplicatePapers(emptySet(), 0.8f)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `When input has a single paper, then it is returned unchanged`() {
            val p = DataBuilder.createExampleFetcherPaper(title = "Solo")
            val result = matcher.deduplicatePapers(setOf(p), 0.8f)
            assertEquals(setOf(p), result)
        }

        @Test
        fun `When two papers are similar above threshold, then they are merged into one`() {
            val a = DataBuilder.createExampleFetcherPaper(title = "Deep Learning Approaches")
            val b = DataBuilder.createExampleFetcherPaper(title = "Deep Learning Approach")
            // similarity > 0 and should exceed a low threshold
            val result = matcher.deduplicatePapers(setOf(a, b), 0.5f)
            assertEquals(1, result.size)
        }

        @Test
        fun `When two papers are dissimilar below threshold, then both are returned`() {
            val a = DataBuilder.createExampleFetcherPaper(title = "Quantum Computing")
            val b = DataBuilder.createExampleFetcherPaper(title = "Medieval History")
            val result = matcher.deduplicatePapers(setOf(a, b), 0.85f)
            assertEquals(2, result.size)
        }

        @Test
        fun `When A is similar to B and B is similar to C but A is not similar to C, then greedy grouping applies`() {
            // A and B merge; C is dissimilar to A (the group representative) → new group
            val a = DataBuilder.createExampleFetcherPaper(title = "aaa bbb")
            val b = DataBuilder.createExampleFetcherPaper(title = "aaa bbc") // similar to a
            val c = DataBuilder.createExampleFetcherPaper(title = "zzz yyy") // dissimilar to a
            // Use ordered set so iteration order is stable: a, b, c
            val ordered = linkedSetOf(a, b, c)
            val result = matcher.deduplicatePapers(ordered, 0.5f)
            assertEquals(2, result.size)
        }

        @Test
        fun `When two papers have conflicting metadata keys, then the first paper's value wins`() {
            val a = DataBuilder.createExampleFetcherPaper(title = "Same Title", fetcherMetadata = mapOf("k" to "v1"))
            val b = DataBuilder.createExampleFetcherPaper(title = "Same Title", fetcherMetadata = mapOf("k" to "v2"))
            val result = matcher.deduplicatePapers(linkedSetOf(a, b), 0.5f)
            assertEquals(1, result.size)
            assertEquals("v1", result.first().fetcherMetadata["k"])
        }

        @ParameterizedTest
        @ValueSource(ints = [42, 69, 85, 100])
        fun `When two papers score exactly at the threshold, then they are merged`(similarityInt: Int) {
            val similarity = similarityInt / 100.0
            val (titleA, titleB) = getSimilarStrings(similarityInt)

            val a = DataBuilder.createExampleFetcherPaper(
                title = titleA,
                abstract = "",
                authors = emptyList(),
            )
            val b = DataBuilder.createExampleFetcherPaper(
                title = titleB,
                abstract = "",
                authors = emptyList(),
            )
            // Guard: the pair really does score exactly at the threshold
            assertEquals(similarity, titleOnlyMatcher.similarity(a, b), DELTA)

            val result = titleOnlyMatcher.deduplicatePapers(linkedSetOf(a, b), similarity.toFloat())

            assertEquals(1, result.size)
        }

        @Test
        fun `When papers have different external IDs of the same type, then the first is kept`() {
            val a = DataBuilder.createExampleFetcherPaper(
                title = "Deep Learning",
                abstract = "",
                authors = emptyList(),
                externalIds = listOf(ExternalId(ExternalIdType.URL, "https://a.example")),
            )
            val b = DataBuilder.createExampleFetcherPaper(
                title = "Deep Learning",
                abstract = "",
                authors = emptyList(),
                externalIds = listOf(ExternalId(ExternalIdType.URL, "https://b.example")),
            )

            val result = matcher.deduplicatePapers(linkedSetOf(a, b), 0.5f)

            assertEquals(1, result.size)
            assertThat(result.first().externalIds).contains(ExternalId(ExternalIdType.URL, "https://a.example"))
        }
    }

    @Nested
    inner class FindMatch {
        private val fetched = DataBuilder.createExampleFetcherPaper(title = "Deep Learning", year = 2020)

        @Test
        fun `When candidates list is empty, then result is null`() {
            assertNull(matcher.findMatch(fetched, emptyList(), 0.8f))
        }

        @Test
        fun `When one candidate is above threshold, then it is returned`() {
            val candidate = DataBuilder.createExamplePaper(title = "Deep Learning", year = 2020)
            val result = matcher.findMatch(fetched, listOf(candidate), 0.5f)
            assertNotNull(result)
            assertEquals(candidate.id, result.id)
        }

        @Test
        fun `When one candidate is below threshold, then result is null`() {
            val candidate = DataBuilder.createExamplePaper(title = "Quantum Physics", year = 2020)
            assertNull(matcher.findMatch(fetched, listOf(candidate), 0.85f))
        }

        @Test
        fun `When multiple candidates exist, then the highest-scoring one above threshold is returned`() {
            val good = DataBuilder.createExamplePaper(title = "Deep Learning", year = 2020)
            val poor = DataBuilder.createExamplePaper(title = "Machine Learning", year = 2020)
            val result = matcher.findMatch(fetched, listOf(poor, good), 0.5f)
            assertNotNull(result)
            assertEquals(good.id, result.id)
        }

        @ParameterizedTest
        @ValueSource(ints = [42, 69, 85, 100])
        fun `When a candidate scores exactly at the threshold, then it is returned`(similarityInt: Int) {
            val similarity = similarityInt / 100.0
            val (titleA, titleB) = getSimilarStrings(similarityInt)

            val fetched = DataBuilder.createExampleFetcherPaper(
                title = titleA,
                abstract = "",
                authors = emptyList(),
            )
            val candidate = DataBuilder.createExamplePaper(
                title = titleB,
                abstract = "",
                authors = emptyList(),
            )
            // Guard: the pair really does score exactly at the threshold
            assertEquals(similarity, titleOnlyMatcher.similarity(fetched, candidate.toFetcherPaper()), DELTA)

            val result = titleOnlyMatcher.findMatch(fetched, listOf(candidate), similarity.toFloat())

            assertNotNull(result)
        }

        @Test
        fun `When a candidate shares only a blank external ID, then it is not matched`() {
            val blankExternalId = listOf(ExternalId(ExternalIdType.DOI, ""))
            val fetched = DataBuilder.createExampleFetcherPaper(
                title = "Quantum Computing",
                abstract = "",
                authors = emptyList(),
                externalIds = blankExternalId,
                year = 2020,
            )
            val unrelatedCandidate = DataBuilder.createExamplePaper(
                title = "Medieval History",
                abstract = "",
                authors = emptyList(),
                externalIds = blankExternalId,
                year = 1850,
            )

            assertNull(matcher.findMatch(fetched, listOf(unrelatedCandidate), 0.85f))
        }
    }

    @Nested
    inner class MergeMetadata {
        @Test
        fun `When fetched and existing metadata have disjoint and overlapping keys, then existing wins on conflict`() {
            val existing = mapOf("b" to "DB", "c" to "3")
            val fetched = mapOf("a" to "1", "b" to "2")
            val result = matcher.mergeMetadata(existing, fetched)
            assertEquals(mapOf("a" to "1", "b" to "DB", "c" to "3"), result)
        }

        @Test
        fun `When fetched metadata is empty, then existing metadata is returned unchanged`() {
            val existing = mapOf("x" to "1")
            val fetched = emptyMap<String, String>()
            assertEquals(mapOf("x" to "1"), matcher.mergeMetadata(existing, fetched))
        }

        @Test
        fun `When existing metadata is empty, then fetched metadata is returned`() {
            val existing = emptyMap<String, String>()
            val fetched = mapOf("a" to "1")
            assertEquals(mapOf("a" to "1"), matcher.mergeMetadata(existing, fetched))
        }
    }
}
