package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.model.dto.toGrpcPapers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import snowballr.Base
import snowballr.boolValue
import snowballr.nothing
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

interface IReadingListService {
    /**
     * Service implementation of [SnowballRService.getReadingList].
     */
    suspend fun getReadingList(): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.isPaperOnReadingList].
     */
    suspend fun isPaperOnReadingList(request: Base.Id): Base.BoolValue

    /**
     * Service implementation of [SnowballRService.addPaperToReadingList].
     */
    suspend fun addPaperToReadingList(request: Base.Id): Base.Nothing

    /**
     * Service implementation of [SnowballRService.removePaperFromReadingList].
     */
    suspend fun removePaperFromReadingList(request: Base.Id): Base.Nothing
}

/**
 * The [ReadingListService] class is responsible for managing reading lists by
 * implementing the [IReadingListService].
 *
 * This class serves as a layer that abstracts the responsibility of user CRUD operations,
 * delegating the actual persistence operations to the [IReadingListTableRepo] repository.
 *
 * @constructor Initializes the [ReadingListService] with the needed repositories.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param paperRepo The repository responsible for managing persistence operations for papers.
 * @param authorOfPaperRepo The repository responsible for managing persistence operations for author-paper associations.
 * @param citationRepo The repository responsible for managing persistence operations for paper citations.
 * @param repo The repository responsible for managing persistence operations for reading list entries.
 */
class ReadingListService(
    private val userRepo: IUserTableRepo,
    private val paperRepo: IPaperTableRepo,
    private val authorOfPaperRepo: IAuthorOfPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
    private val repo: IReadingListTableRepo,
) : IReadingListService {
    /**
     * Throws a [NotFoundException] if the paper with the passed [id] doesn't exist.
     */
    private suspend fun throwIfPaperDoesNotExist(id: UUID) {
        if (!paperRepo.doesPaperExistById(id)) {
            throw NotFoundException(EntityType.PAPER, id.toString())
        }
    }

    override suspend fun getReadingList(): GrpcPaper.List = withUser(userRepo) { currentUser ->
        val papers = repo.getAllReadingListEntries(currentUser.id).map { paper ->
            val authors = authorOfPaperRepo.getAuthorsOfPaperById(paper.id).map { it.toGrpcAuthor() }
            val backwardReferences = citationRepo
                .getBackwardsReferencedPaperIdsOfPaperById(paper.id)
                .map(UUID::toString)
            paper.toGrpcPaper(authors, backwardReferences)
        }

        papers.toGrpcPapers()
    }

    override suspend fun isPaperOnReadingList(request: Base.Id): Base.BoolValue = withUser(userRepo) { currentUser ->
        val paperId = parseUUID(request.id, EntityType.PAPER)
        throwIfPaperDoesNotExist(paperId)

        boolValue { value = repo.isPaperOnReadingList(currentUser.id, paperId) }
    }

    override suspend fun addPaperToReadingList(request: Base.Id): Base.Nothing = withUser(userRepo) { currentUser ->
        val paperId = parseUUID(request.id, EntityType.PAPER)

        throwIfPaperDoesNotExist(paperId)
        repo.createReadingListEntry(currentUser.id, paperId)

        nothing { }
    }

    override suspend fun removePaperFromReadingList(request: Base.Id): Base.Nothing =
        withUser(userRepo) { currentUser ->
            val paperId = parseUUID(request.id, EntityType.PAPER)

            throwIfPaperDoesNotExist(paperId)
            repo.removeReadingListEntry(currentUser.id, paperId)

            nothing { }
        }
}
