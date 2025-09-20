package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaperTableRepoTest : RepositoryTest(arrayOf(PaperTable), false) {
    private val repo = PaperTableRepo(db)

    @Nested
    inner class GetPaperById {
        @Test
        fun `When a paper is found, then a successful result with the correct paper is returned`() = runTest {
            val paperId = insertPaperAndGetId(externalId = "ExternalId")
            val result = repo.getPaperById(paperId)

            val paper = assertResultSuccess(result)
            with(paper) {
                assertEquals("Title", title)
                assertEquals("ExternalId", externalId)
                assertEquals("Abstract", abstract)
                assertEquals(2025, year)
                assertEquals("Publisher", publisher)
                assertEquals("PublicationType", publicationType)
                assertEquals("PublicationName", publicationName)
                assertThat(fetcherMetadata).isEmpty()
            }
        }

        @Test
        fun `When a paper is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getPaperById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }

        @Test
        fun `When a paper is existent, then doesPaperExistById returns true`() = runTest {
            val paperId = insertPaperAndGetId()
            assertTrue { repo.doesPaperExistById(paperId) }
        }

        @Test
        fun `When a paper is non-existent, then doesPaperExistById returns false`() = runTest {
            assertFalse { repo.doesPaperExistById(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class DoesPaperExistById {
        @Test
        fun `When a paper with the given id exists, then true returned`() = runTest {
            val paperId =
                insertPaperAndGetId("Test Paper")
            val isPaperExistent = repo.doesPaperExistById(paperId)

            assertTrue(isPaperExistent)
        }

        @Test
        fun `When a paper with the given id does not exist, then false returned`() = runTest {
            val paperId = UUID.randomUUID()
            val isPaperExistent = repo.doesPaperExistById(paperId)

            assertFalse(isPaperExistent)
        }
    }
}
