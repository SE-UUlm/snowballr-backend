package se.uulm.snowballr.backend.fetcher

import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.dto.Author
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FetcherPaperTest {
    @Test
    fun `When converting a FetcherPaper to a Paper, then all fields are mapped as expected`() {
        val beforeConversion = OffsetDateTime.now()
        val fetcherPaper = FetcherPaper(
            title = "Example title",
            externalId = "external-123",
            abstract = "Example abstract",
            year = 2024,
            publisher = "Example publisher",
            publicationType = "journal",
            publicationName = "Example publication",
            authors = listOf(Author("Ada", "Lovelace")),
            metadata = mapOf("source" to "xplore"),
        )

        val paper = fetcherPaper.toPaper()
        val afterConversion = OffsetDateTime.now()

        assertNotNull(paper.id)
        assertNotEquals("00000000-0000-0000-0000-000000000000", paper.id.toString())
        assertEquals(fetcherPaper.title, paper.title)
        assertEquals(fetcherPaper.externalId, paper.externalId)
        assertEquals(fetcherPaper.abstract, paper.abstract)
        assertEquals(fetcherPaper.year, paper.year)
        assertEquals(fetcherPaper.publisher, paper.publisher)
        assertEquals(fetcherPaper.publicationType, paper.publicationType)
        assertEquals(fetcherPaper.publicationName, paper.publicationName)
        assertEquals(fetcherPaper.authors, paper.authors)
        assertEquals(fetcherPaper.metadata, paper.fetcherMetadata)
        assertNull(paper.pdfId)
        assertNull(paper.modifiedAt)
        assertNull(paper.modifiedBy)
        assertFalse(paper.createdAt.isBefore(beforeConversion))
        assertFalse(paper.createdAt.isAfter(afterConversion))
    }
}
