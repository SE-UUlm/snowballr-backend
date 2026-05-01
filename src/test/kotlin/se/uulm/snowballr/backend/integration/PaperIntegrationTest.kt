package se.uulm.snowballr.backend.integration

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.PaperOuterClass.Paper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaperIntegrationTest : IntegrationTest() {
    @Nested
    inner class CreatePaper {
        @Test
        fun `When a paper is created, then it can be retrieved by ID`() = runTest {
            val paper = createPaper("My Paper")
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            val fetched = mainService.getPaperById(paperId)

            assertEquals(paper.id, fetched.id)
            assertEquals("My Paper", fetched.title)
        }

        @Test
        fun `When a paper with an external ID is created, then creating another with the same external ID fails`() =
            runTest {
                createPaper(externalId = "ext-123")

                assertThrows<DuplicatePaperException> { createPaper(externalId = "ext-123") }
            }

        @Test
        fun `When two papers have different external IDs, then both can be created successfully`() = runTest {
            val first = createPaper(title = "Paper A", externalId = "ext-a")
            val second = createPaper(title = "Paper B", externalId = "ext-b")

            assertNotNull(first.id)
            assertNotNull(second.id)
        }
    }

    @Nested
    inner class UpdatePaper {
        @Test
        fun `When a paper's title is updated, then the updated title is persisted`() = runTest {
            val paper = createPaper("Original Title")
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            val request = Paper.Update.newBuilder()
                .setPaper(paper.toBuilder().setTitle("Updated Title").build())
                .setMask(FieldMaskUtil.fromStringList(listOf("paper.title")))
                .build()

            val result = mainService.updatePaper(request)

            assertEquals("Updated Title", result.title)

            val fetched = mainService.getPaperById(paperId)
            assertEquals("Updated Title", fetched.title)
        }

        @Test
        fun `When a paper's year is updated, then the updated year is persisted`() = runTest {
            val paper = createPaper()
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            val request = Paper.Update.newBuilder()
                .setPaper(paper.toBuilder().setYear(2000).build())
                .setMask(FieldMaskUtil.fromStringList(listOf("paper.year")))
                .build()

            mainService.updatePaper(request)

            val fetched = mainService.getPaperById(paperId)
            assertEquals(2000, fetched.year)
        }

        @Test
        fun `When a paper is updated with a duplicate external ID, then the update fails`() = runTest {
            createPaper(externalId = "taken-id")
            val other = createPaper(externalId = "other-id")

            val request = Paper.Update.newBuilder()
                .setPaper(other.toBuilder().setExternalId("taken-id").build())
                .setMask(FieldMaskUtil.fromStringList(listOf("paper.external_id")))
                .build()

            assertThrows<DuplicatePaperException> { mainService.updatePaper(request) }
        }
    }
}
