package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.table.association.CitationTable
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [CitationTable].
 *
 * This interface provides abstraction for handling persistence and retrieval operations for citations of papers. By
 * using this interface, the functionality for managing papers can remain decoupled from the specifics of the database
 * layer.
 */
interface ICitationTableRepo {
    /**
     * Returns the backward references of a paper by its ID.
     */
    suspend fun getBackwardsReferencedPaperIdsOfPaperById(id: UUID): List<UUID>

    /**
     * Returns the forward references of a paper by its ID.
     */
    suspend fun getForwardReferencedPaperIdsOfPaperById(id: UUID): List<UUID>

    /**
     * Adds a backward reference: records that paper [id] cites [referencedPaperId].
     */
    suspend fun addBackwardReferencedPaper(id: UUID, referencedPaperId: UUID)

    /**
     * Adds a forward reference: records that paper [citingPaperId] cites paper [id].
     */
    suspend fun addForwardReferencedPaper(id: UUID, citingPaperId: UUID)
}

/**
 * Repository implementation for managing the [CitationTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for citations of papers by
 * leveraging the database abstraction defined in [IDatabase]. It facilitates CRUD operations on citations of papers and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class CitationTableRepo(
    private val db: IDatabase,
) : ICitationTableRepo {
    override suspend fun getBackwardsReferencedPaperIdsOfPaperById(id: UUID): List<UUID> = db.query {
        CitationTable
            .select(CitationTable.citedPaperId)
            .where { CitationTable.paperId eq id }
            .map { it[CitationTable.citedPaperId].value }
            .toList()
    }

    override suspend fun getForwardReferencedPaperIdsOfPaperById(id: UUID): List<UUID> = db.query {
        CitationTable
            .select(CitationTable.paperId)
            .where { CitationTable.citedPaperId eq id }
            .map { it[CitationTable.paperId].value }
            .toList()
    }

    override suspend fun addBackwardReferencedPaper(id: UUID, referencedPaperId: UUID) =
        addReferencedPaper(id, referencedPaperId)

    override suspend fun addForwardReferencedPaper(id: UUID, citingPaperId: UUID) =
        addReferencedPaper(citingPaperId, id)

    private suspend fun addReferencedPaper(citingPaperId: UUID, citedPaperId: UUID) {
        db.query {
            CitationTable.insert {
                it[CitationTable.paperId] = citingPaperId
                it[CitationTable.citedPaperId] = citedPaperId
            }
        }
    }
}
