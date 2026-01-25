package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.toGrpcPapers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import snowballr.Base
import snowballr.PaperOuterClass.Paper as GrpcPaper

interface IReadingListService {
    /**
     * Service implementation of [SnowballRService.getReadingList].
     */
    suspend fun getReadingList(): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.isPaperOnReadingList].
     */
    suspend fun isPaperOnReadingList(request: Base.Id): Boolean

    /**
     * Service implementation of [SnowballRService.addPaperToReadingList].
     */
    suspend fun addPaperToReadingList(request: Base.Id)

    /**
     * Service implementation of [SnowballRService.removePaperFromReadingList].
     */
    suspend fun removePaperFromReadingList(request: Base.Id)
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
    override suspend fun getReadingList(): GrpcPaper.List = withUser(userRepo) { currentUser ->
        val papers = repo.getAllReadingListEntries(currentUser.id).map { paper ->
            paper.toGrpcPaperWithAuthorsAndBackwardReferences(citationRepo)
        }

        papers.toGrpcPapers()
    }

    override suspend fun isPaperOnReadingList(request: Base.Id): Boolean = withUser(userRepo) { currentUser ->
        val paperId = parseUUID(request.id, EntityType.PAPER)

        paperRepo.ensurePaperExists(paperId)

        repo.isPaperOnReadingList(currentUser.id, paperId)
    }

    override suspend fun addPaperToReadingList(request: Base.Id) = withUser(userRepo) { currentUser ->
        val paperId = parseUUID(request.id, EntityType.PAPER)

        paperRepo.ensurePaperExists(paperId)

        repo.createReadingListEntry(currentUser.id, paperId)
    }

    override suspend fun removePaperFromReadingList(request: Base.Id) = withUser(userRepo) { currentUser ->
        val paperId = parseUUID(request.id, EntityType.PAPER)

        paperRepo.ensurePaperExists(paperId)

        repo.removeReadingListEntry(currentUser.id, paperId)
    }
}
