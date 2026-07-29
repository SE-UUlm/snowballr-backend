package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import java.util.UUID

private val logger = KotlinLogging.logger {}

interface IReadingListService {
    /**
     * Service implementation of [SnowballRService.getReadingList].
     */
    suspend fun getReadingList(): List<PaperResponse>

    /**
     * Service implementation of [SnowballRService.isPaperOnReadingList].
     */
    suspend fun isPaperOnReadingList(paperId: UUID): Boolean

    /**
     * Service implementation of [SnowballRService.addPaperToReadingList].
     */
    suspend fun addPaperToReadingList(paperId: UUID)

    /**
     * Service implementation of [SnowballRService.removePaperFromReadingList].
     */
    suspend fun removePaperFromReadingList(paperId: UUID)
}

/**
 * The [ReadingListService] class is responsible for managing reading lists by
 * implementing the [IReadingListService].
 *
 * This class serves as a layer that abstracts the responsibility of user CRUD operations,
 * delegating the actual persistence operations to the [IReadingListTableRepo] repository.
 *
 * @constructor Initializes the [ReadingListService] with the necessary repositories.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param paperRepo The repository responsible for managing persistence operations for papers.
 * @param citationRepo The repository responsible for managing persistence operations for paper citations.
 * @param repo The repository responsible for managing persistence operations for reading list entries.
 */
class ReadingListService(
    private val userRepo: IUserTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
    private val repo: IReadingListTableRepo,
) : IReadingListService {
    override suspend fun getReadingList(): List<PaperResponse> = withUser(userRepo) { currentUser ->
        repo.getAllReadingListEntries(currentUser.id).map { it.toPaperResponse(citationRepo) }
    }

    override suspend fun isPaperOnReadingList(paperId: UUID): Boolean = withUser(userRepo) { currentUser ->
        paperRepo.ensurePaperExists(paperId)

        repo.isPaperOnReadingList(currentUser.id, paperId)
    }

    override suspend fun addPaperToReadingList(paperId: UUID) = withUser(userRepo) { currentUser ->
        paperRepo.ensurePaperExists(paperId)

        repo.createReadingListEntry(currentUser.id, paperId)
        logger.info { "Paper $paperId added to reading list" }
    }

    override suspend fun removePaperFromReadingList(paperId: UUID) = withUser(userRepo) { currentUser ->
        paperRepo.ensurePaperExists(paperId)

        repo.removeReadingListEntry(currentUser.id, paperId)
        logger.info { "Paper $paperId removed from reading list" }
    }
}
