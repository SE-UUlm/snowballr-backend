package se.uulm.snowballr.backend.normalization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType

class PaperNormalizerTest {
    @Nested
    inner class NormalizeText {
        @Test
        fun `When a string has leading and trailing whitespace, then it is trimmed`() {
            assertEquals("Title", PaperNormalizer.normalizeText("  Title  "))
        }

        @Test
        @Suppress("StringShouldBeRawString")
        fun `When a string has internal runs of mixed whitespace, then they collapse to a single space`() {
            assertEquals(
                "A Study Of Things",
                PaperNormalizer.normalizeText("A  Study\tOf\n\nThings"),
            )
        }

        @Test
        fun `When a string contains non-breaking spaces, then they are collapsed like regular whitespace`() {
            assertEquals("A B", PaperNormalizer.normalizeText("A  B"))
        }

        @Test
        fun `When a string contains curly quotes, then they are folded to straight quotes`() {
            assertEquals("\"Don't\"", PaperNormalizer.normalizeText("“Don’t”"))
        }

        @Test
        fun `When a string contains dash variants, then they are folded to a hyphen-minus`() {
            assertEquals("A-B-C", PaperNormalizer.normalizeText("A–B—C"))
            assertEquals("-5", PaperNormalizer.normalizeText("−5"))
        }

        @Test
        fun `When a string is NFD-decomposed, then it is normalized to NFC`() {
            val decomposed = "e\u0301clair" // "e" + combining acute accent (U+0301)
            val precomposed = "\u00e9clair" // single precomposed code point (U+00E9)

            assertEquals(precomposed, PaperNormalizer.normalizeText(decomposed))
        }

        @Test
        fun `When two differently-encoded but visually identical strings are normalized, then they become equal`() {
            val decomposed = "Cafe\u0301"
            val precomposed = "Caf\u00e9"

            assertEquals(PaperNormalizer.normalizeText(precomposed), PaperNormalizer.normalizeText(decomposed))
        }

        @Test
        fun `When a text contains invisible characters, then they are removed`() {
            val text = "\u00ADFoo\u200B"
            val expected = "Foo"

            assertEquals(expected, PaperNormalizer.normalizeText(text))
        }
    }

    @Nested
    inner class NormalizeAuthors {
        @Test
        fun `When an author has blank first and last name, then it is dropped`() {
            val paper = DataBuilder.createExampleFetcherPaper(
                authors = listOf(
                    DataBuilder.createExampleAuthor(firstName = "  ", lastName = ""),
                    DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Doe"),
                ),
            )

            val result = PaperNormalizer.normalize(paper)

            assertEquals(listOf(DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Doe")), result.authors)
        }

        @Test
        fun `When an author has whitespace-only formatting, then their names are trimmed and collapsed`() {
            val paper = DataBuilder.createExampleFetcherPaper(
                authors = listOf(DataBuilder.createExampleAuthor(firstName = "  Jane  ", lastName = "Do\te")),
            )

            val result = PaperNormalizer.normalize(paper)

            assertEquals(listOf(DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Do e")), result.authors)
        }

        @Test
        fun `When normalizeAuthors is called directly, then blank authors are dropped and the rest are normalized`() {
            val authors = listOf(
                DataBuilder.createExampleAuthor(firstName = "  Jane  ", lastName = "Doe"),
                DataBuilder.createExampleAuthor(firstName = "", lastName = " "),
            )

            val result = PaperNormalizer.normalizeAuthors(authors)

            assertEquals(listOf(DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Doe")), result)
        }
    }

    @Nested
    inner class NormalizeExternalIds {
        @Test
        fun `When an external ID value is blank after trimming, then it is dropped`() {
            val paper = DataBuilder.createExampleFetcherPaper(
                externalIds = listOf(
                    DataBuilder.createExampleExternalId(type = ExternalIdType.DOI, value = "   "),
                    DataBuilder.createExampleExternalId(type = ExternalIdType.ARXIV, value = "1234.5678"),
                ),
            )

            val result = PaperNormalizer.normalize(paper)

            assertEquals(
                listOf(DataBuilder.createExampleExternalId(type = ExternalIdType.ARXIV, value = "1234.5678")),
                result.externalIds,
            )
        }

        @Test
        fun `When an external ID value has surrounding whitespace, then it is trimmed`() {
            val paper = DataBuilder.createExampleFetcherPaper(
                externalIds = listOf(DataBuilder.createExampleExternalId(value = "  10.1234/5678  ")),
            )

            val result = PaperNormalizer.normalize(paper)

            assertEquals("10.1234/5678", result.externalIds.single().value)
        }

        @Test
        fun `When an external ID value contains punctuation that would be folded in prose, then it is left untouched`() {
            val paper = DataBuilder.createExampleFetcherPaper(
                externalIds = listOf(DataBuilder.createExampleExternalId(value = "10.1234/foo–bar")),
            )

            val result = PaperNormalizer.normalize(paper)

            assertEquals("10.1234/foo–bar", result.externalIds.single().value)
        }

        @Test
        fun `When normalizeExternalIds is called directly, then blank values are dropped and the rest are trimmed`() {
            val externalIds = listOf(
                ExternalId(ExternalIdType.DOI, "  10.1234/5678  "),
                ExternalId(ExternalIdType.ARXIV, "   "),
            )

            val result = PaperNormalizer.normalizeExternalIds(externalIds)

            assertEquals(listOf(ExternalId(ExternalIdType.DOI, "10.1234/5678")), result)
        }
    }

    @Nested
    inner class NormalizeFullPaper {
        @Test
        @Suppress("StringShouldBeRawString")
        fun `When a fetcher paper has messy formatting throughout, then all fields are normalized consistently`() {
            val paper = DataBuilder.createExampleFetcherPaper(
                title = "  The “Great” Study — Part  1  ",
                abstract = "This\n\nis\tan   abstract.",
                publisher = "  ACME  Press  ",
                publicationType = "Conference  Paper",
                publicationName = "  Proceedings of X  ",
                authors = listOf(
                    DataBuilder.createExampleAuthor(firstName = " Jane ", lastName = "Doe"),
                    DataBuilder.createExampleAuthor(firstName = "", lastName = " "),
                ),
                externalIds = listOf(
                    DataBuilder.createExampleExternalId(type = ExternalIdType.DOI, value = " 10.1234/5678 "),
                    DataBuilder.createExampleExternalId(type = ExternalIdType.ARXIV, value = ""),
                ),
            )

            val result = PaperNormalizer.normalize(paper)

            assertEquals("The \"Great\" Study - Part 1", result.title)
            assertEquals("This is an abstract.", result.abstract)
            assertEquals("ACME Press", result.publisher)
            assertEquals("Conference Paper", result.publicationType)
            assertEquals("Proceedings of X", result.publicationName)
            assertEquals(listOf(DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Doe")), result.authors)
            assertEquals(
                listOf(DataBuilder.createExampleExternalId(type = ExternalIdType.DOI, value = "10.1234/5678")),
                result.externalIds,
            )
            assertEquals(paper.year, result.year)
            assertEquals(paper.fetcherMetadata, result.fetcherMetadata)
        }
    }
}
