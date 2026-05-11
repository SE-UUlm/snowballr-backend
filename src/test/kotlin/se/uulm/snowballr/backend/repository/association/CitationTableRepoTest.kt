package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.CitationTable
import java.sql.SQLException
import java.util.UUID

class CitationTableRepoTest : RepositoryTest(arrayOf(PaperTable, CitationTable), false) {
    private val repo = CitationTableRepo(db)

    private suspend fun addCitation(paperId: UUID, referencedPaperId: UUID) = db.query {
        CitationTable.insert {
            it[CitationTable.paperId] = paperId
            it[CitationTable.citedPaperId] = referencedPaperId
        }
    }

    @Nested
    inner class GetBackwardsReferencedPaperIdsOfPaperById {
        @Test
        fun `When a paper has no backward references, then none are returned`() = runTest {
            val paperId = insertPaperAndGetId()

            val backwardReferencedPaperIds = repo.getBackwardsReferencedPaperIdsOfPaperById(paperId)

            assertThat(backwardReferencedPaperIds).isEmpty()
        }

        @Test
        fun `When a paper has one backward reference, then it is returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val paperId2 = insertPaperAndGetId(externalId = "ExternalId2")
            addCitation(paperId, paperId2)

            val actualReferences = repo.getBackwardsReferencedPaperIdsOfPaperById(paperId)

            assertThat(actualReferences).hasSize(1)
            assertThat(actualReferences).containsExactly(paperId2)
        }

        @Test
        fun `When a paper has two backward references, then they are returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val paperId2 = insertPaperAndGetId(externalId = "ExternalId2")
            val paperId3 = insertPaperAndGetId(externalId = "ExternalId3")
            addCitation(paperId, paperId2)
            addCitation(paperId, paperId3)

            val actualReferences = repo.getBackwardsReferencedPaperIdsOfPaperById(paperId)

            assertThat(actualReferences).hasSize(2)
            assertThat(actualReferences).containsExactlyInAnyOrder(paperId2, paperId3)
        }
    }

    @Nested
    inner class GetForwardReferencedPaperIdsOfPaperById {
        @Test
        fun `When a paper has no forward references, then none are returned`() = runTest {
            val paperId = insertPaperAndGetId()

            val forwardReferencedPaperIds = repo.getForwardReferencedPaperIdsOfPaperById(paperId)

            assertThat(forwardReferencedPaperIds).isEmpty()
        }

        @Test
        fun `When a paper has one forward reference, then it is returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val citedPaperId = insertPaperAndGetId(externalId = "ExternalId2")
            addCitation(paperId, citedPaperId)

            val actualReferences = repo.getForwardReferencedPaperIdsOfPaperById(citedPaperId)

            assertThat(actualReferences).hasSize(1)
            assertThat(actualReferences).containsExactly(paperId)
        }

        @Test
        fun `When a paper has two forward references, then they are returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val paperId2 = insertPaperAndGetId(externalId = "ExternalId2")
            val citedPaperId = insertPaperAndGetId(externalId = "ExternalId3")
            addCitation(paperId, citedPaperId)
            addCitation(paperId2, citedPaperId)

            val actualReferences = repo.getForwardReferencedPaperIdsOfPaperById(citedPaperId)

            assertThat(actualReferences).hasSize(2)
            assertThat(actualReferences).containsExactlyInAnyOrder(paperId, paperId2)
        }
    }

    @Nested
    inner class AddBackwardReferencedPaper {
        @Test
        fun `When adding a backward reference, then it appears in the backward references`() = runTest {
            val paperId = insertPaperAndGetId()
            val referencedPaperId = insertPaperAndGetId(externalId = "ExternalId2")

            repo.addBackwardReferencedPaper(paperId, referencedPaperId)

            val actualReferences = repo.getBackwardsReferencedPaperIdsOfPaperById(paperId)
            assertThat(actualReferences).containsExactly(referencedPaperId)
        }

        @Test
        fun `When adding the same backward reference twice, then an SQLException is thrown`() = runTest {
            val paperId = insertPaperAndGetId()
            val referencedPaperId = insertPaperAndGetId(externalId = "ExternalId2")

            repo.addBackwardReferencedPaper(paperId, referencedPaperId)

            assertThrows<SQLException> { repo.addBackwardReferencedPaper(paperId, referencedPaperId) }
        }
    }

    @Nested
    inner class AddForwardReferencedPaper {
        @Test
        fun `When adding a forward reference, then it appears in the forward references`() = runTest {
            val paperId = insertPaperAndGetId()
            val citingPaperId = insertPaperAndGetId(externalId = "ExternalId2")

            repo.addForwardReferencedPaper(paperId, citingPaperId)

            val actualReferences = repo.getForwardReferencedPaperIdsOfPaperById(paperId)
            assertThat(actualReferences).containsExactly(citingPaperId)
        }

        @Test
        fun `When adding the same forward reference twice, then an SQLException is thrown`() = runTest {
            val paperId = insertPaperAndGetId()
            val citingPaperId = insertPaperAndGetId(externalId = "ExternalId2")

            repo.addForwardReferencedPaper(paperId, citingPaperId)

            assertThrows<SQLException> { repo.addForwardReferencedPaper(paperId, citingPaperId) }
        }
    }
}
