package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.table.PaperTable
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaperTableRepoTest : RepositoryTest(arrayOf(PaperTable), false) {
    private val repo = PaperTableRepo(db)

    @Nested
    inner class GetPaperById {
        @Test
        fun `When a paper is found, then the correct paper is returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val paper = repo.getPaperById(paperId)

            with(paper) {
                assertThat(title).isEqualTo("Title")
                assertThat(externalId).isEqualTo("ExternalId")
                assertThat(abstract).isEqualTo("Abstract")
                assertThat(publishedAt).isEqualTo(Instant.fromEpochSeconds(0))
                assertThat(publisher).isEqualTo("Publisher")
                assertThat(publicationType).isEqualTo("PublicationType")
                assertThat(publicationName).isEqualTo("PublicationName")
                assertThat(fetcherMetadata).isEmpty()
            }
        }

        @Test
        fun `When a paper is not found, then an exception is thrown`() = runTest {
            assertThrows<NotFoundException> { repo.getPaperById(UUID.randomUUID()) }
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
}
