package se.uulm.snowballr.backend.normalization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest

class PaperRequestNormalizationTest {
    @Nested
    inner class CreatePaperRequestNormalized {
        @Test
        @Suppress("StringShouldBeRawString")
        fun `When a create request has messy formatting, then it is normalized`() {
            val paper = DataBuilder.createExamplePaper(
                title = "  Messy   Title  ",
                abstract = "Multi\n\nline   abstract",
                publisher = "  Messy Publisher  ",
                publicationType = "  Messy  Type  ",
                publicationName = "  Messy  Name  ",
                authors = listOf(
                    DataBuilder.createExampleAuthor(firstName = "  Jane  ", lastName = "Doe"),
                    DataBuilder.createExampleAuthor(firstName = "", lastName = " "),
                ),
                externalIds = listOf(
                    ExternalId(ExternalIdType.DOI, "  10.1234/messy  "),
                    ExternalId(ExternalIdType.ARXIV, "   "),
                ),
            )
            val request = CreatePaperRequest.fromPaper(paper)

            val result = request.normalized()

            assertEquals("Messy Title", result.title)
            assertEquals("Multi line abstract", result.abstract)
            assertEquals("Messy Publisher", result.publisher)
            assertEquals("Messy Type", result.publicationType)
            assertEquals("Messy Name", result.publicationName)
            assertEquals(listOf(DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Doe")), result.authors)
            assertEquals(listOf(ExternalId(ExternalIdType.DOI, "10.1234/messy")), result.externalIds)
        }

        @Test
        fun `When a create request is already clean, then normalization is a no-op`() {
            val request = CreatePaperRequest.fromPaper(DataBuilder.createExamplePaper())

            assertEquals(request, request.normalized())
        }
    }

    @Nested
    inner class UpdatePaperRequestNormalized {
        @Test
        @Suppress("StringShouldBeRawString")
        fun `When an update request has messy formatting, then it is normalized`() {
            val paper = DataBuilder.createExamplePaper(
                title = "  Messy   Title  ",
                abstract = "Multi\n\nline   abstract",
                publisher = "  Messy Publisher  ",
                publicationType = "  Messy  Type  ",
                publicationName = "  Messy  Name  ",
                authors = listOf(
                    DataBuilder.createExampleAuthor(firstName = "  Jane  ", lastName = "Doe"),
                    DataBuilder.createExampleAuthor(firstName = "", lastName = " "),
                ),
                externalIds = listOf(
                    ExternalId(ExternalIdType.DOI, "  10.1234/messy  "),
                    ExternalId(ExternalIdType.ARXIV, "   "),
                ),
            )
            val request = UpdatePaperRequest.fromPaper(paper)

            val result = request.normalized()

            assertEquals("Messy Title", result.title)
            assertEquals("Multi line abstract", result.abstract)
            assertEquals("Messy Publisher", result.publisher)
            assertEquals("Messy Type", result.publicationType)
            assertEquals("Messy Name", result.publicationName)
            assertEquals(listOf(DataBuilder.createExampleAuthor(firstName = "Jane", lastName = "Doe")), result.authors)
            assertEquals(listOf(ExternalId(ExternalIdType.DOI, "10.1234/messy")), result.externalIds)
        }

        @Test
        fun `When an update request is already clean, then normalization is a no-op`() {
            val request = UpdatePaperRequest.fromPaper(DataBuilder.createExamplePaper())

            assertEquals(request, request.normalized())
        }
    }
}
