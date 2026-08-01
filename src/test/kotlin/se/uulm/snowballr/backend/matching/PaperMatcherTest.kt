package se.uulm.snowballr.backend.matching

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType

private const val DELTA = 1e-9

class PaperMatcherTest {
    private val defaultConfig = PaperMatchingConfig(1, 0.6, 0.3, 0.1)
    private val matcher = PaperMatcher(defaultConfig)

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

        @Test
        fun `When both author lists are empty, then authors component is dropped`() {
            val a1 = DataBuilder.createExampleFetcherPaper(
                title = "Some random title",
                authors = emptyList(),
                abstract = "Some random abstract",
            )
            val b1 = a1.copy(title = a1.title.reversed(), abstract = a1.abstract.reversed())
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
    }

    @Nested
    inner class MergeMetadata {
        @Test
        fun `When fetched and DB metadata have disjoint and overlapping keys, then DB wins on conflict`() {
            val dbPaper = DataBuilder.createExamplePaper(fetcherMetadata = mapOf("b" to "DB", "c" to "3"))
            val fetched = DataBuilder.createExampleFetcherPaper(fetcherMetadata = mapOf("a" to "1", "b" to "2"))
            val result = matcher.mergeMetadata(dbPaper, fetched)
            assertEquals(mapOf("a" to "1", "b" to "DB", "c" to "3"), result)
        }

        @Test
        fun `When fetched metadata is empty, then DB metadata is returned unchanged`() {
            val dbPaper = DataBuilder.createExamplePaper(fetcherMetadata = mapOf("x" to "1"))
            val fetched = DataBuilder.createExampleFetcherPaper(fetcherMetadata = emptyMap())
            assertEquals(mapOf("x" to "1"), matcher.mergeMetadata(dbPaper, fetched))
        }

        @Test
        fun `When DB metadata is empty, then fetched metadata is returned`() {
            val dbPaper = DataBuilder.createExamplePaper(fetcherMetadata = emptyMap())
            val fetched = DataBuilder.createExampleFetcherPaper(fetcherMetadata = mapOf("a" to "1"))
            assertEquals(mapOf("a" to "1"), matcher.mergeMetadata(dbPaper, fetched))
        }
    }
}
