package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.CitationTable
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
            assertThat(repo.getBackwardsReferencedPaperIdsOfPaperById(paperId)).isEmpty()
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
            assertThat(repo.getForwardReferencedPaperIdsOfPaperById(paperId)).isEmpty()
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
}
