package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.incoming.paper.PaperField
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest

class PaperIntegrationTest : IntegrationTest() {
    @Nested
    inner class CreatePaper {
        @Test
        fun `When a paper is created, then it can be retrieved by ID`() = runTest {
            val paper = createPaper("My Paper")

            val fetched = paperService.getPaperById(paper.id)

            assertEquals(paper.id, fetched.id)
            assertEquals("My Paper", fetched.title)
        }

        @Test
        fun `When a paper with an external ID is created, then creating another with the same external ID fails`() =
            runTest {
                val externalId = DataBuilder.createExampleExternalId()
                createPaper(externalIds = listOf(externalId))

                assertThrows<DuplicatePaperException> { createPaper(externalIds = listOf(externalId)) }
            }

        @Test
        fun `When two papers have different external IDs, then both can be created successfully`() = runTest {
            val externalId1 = DataBuilder.createExampleExternalId(value = "ext-a")
            val first = createPaper(title = "Paper A", externalIds = listOf(externalId1))
            val externalId2 = DataBuilder.createExampleExternalId(value = "ext-b")
            val second = createPaper(title = "Paper B", externalIds = listOf(externalId2))

            assertNotNull(first.id)
            assertNotNull(second.id)
        }
    }

    @Nested
    inner class UpdatePaper {
        @Test
        fun `When a paper's title is updated, then the updated title is persisted`() = runTest {
            val paper = createPaper("Original Title")
            val request = UpdatePaperRequest.fromPaperResponse(paper).copy(title = "Updated Title")

            val result = paperService.updatePaper(request, listOf(PaperField.TITLE))

            assertEquals("Updated Title", result.title)

            val fetched = paperService.getPaperById(paper.id)
            assertEquals("Updated Title", fetched.title)
        }

        @Test
        fun `When a paper's year is updated, then the updated year is persisted`() = runTest {
            val paper = createPaper()
            val request = UpdatePaperRequest.fromPaperResponse(paper).copy(year = 2000)

            paperService.updatePaper(request, listOf(PaperField.YEAR))

            val fetched = paperService.getPaperById(paper.id)
            assertEquals(2000, fetched.year)
        }

        @Test
        fun `When a paper is updated with a duplicate external ID, then the update fails`() = runTest {
            val externalId = DataBuilder.createExampleExternalId(value = "taken-id")
            createPaper(externalIds = listOf(externalId))
            val otherExternalId = DataBuilder.createExampleExternalId(value = "other-id")
            val other = createPaper(externalIds = listOf(otherExternalId))
            val request = UpdatePaperRequest.fromPaperResponse(other).copy(externalIds = listOf(externalId))

            assertThrows<DuplicatePaperException> { paperService.updatePaper(request, listOf(PaperField.EXTERNAL_IDS)) }
        }
    }
}
