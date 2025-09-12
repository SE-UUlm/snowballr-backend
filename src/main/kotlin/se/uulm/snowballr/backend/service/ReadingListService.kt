package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import snowballr.Base
import snowballr.PaperOuterClass
import java.util.UUID

interface IReadingListService {
    /**
     * Service implementation of [SnowballRService.getReadingList].
     */
    suspend fun getReadingList(): PaperOuterClass.Paper.List

    /**
     * Service implementation of [SnowballRService.isPaperOnReadingList].
     */
    suspend fun isPaperOnReadingList(paperId: Base.Id): Base.BoolValue

    /**
     * Service implementation of [SnowballRService.addPaperToReadingList].
     */
    suspend fun addPaperToReadingList(paperId: Base.Id): Base.Nothing

    /**
     * Service implementation of [SnowballRService.removePaperFromReadingList].
     */
    suspend fun removePaperFromReadingList(paperId: Base.Id): Base.Nothing
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

    override suspend fun getReadingList(): PaperOuterClass.Paper.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val papers = repo.getAllReadingListEntries(currentUser.id).map { paper ->
            val authors = authorOfPaperRepo.getAuthorsOfPaperById(paper.id).map { it.toGrpcAuthor() }
            val backwardReferences = citationRepo.getBackwardsReferencedPaperIdsOfPaperById(
                paper.id,
            ).map { it.toString() }
            paper.toGrpcPaper(authors, backwardReferences)
        }
        return PaperOuterClass.Paper.List
            .newBuilder()
            .addAllPapers(papers)
            .build()
    }

    override suspend fun isPaperOnReadingList(paperId: Base.Id): Base.BoolValue {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val paperUuid = parseUUID(paperId.id, EntityType.PAPER)
        throwIfPaperDoesNotExist(paperUuid)
        return Base.BoolValue
            .newBuilder()
            .setValue(
                repo.isPaperOnReadingList(
                    currentUser.id,
                    paperUuid,
                ),
            )
            .build()
    }

    override suspend fun addPaperToReadingList(paperId: Base.Id): Base.Nothing {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val paperUuid = parseUUID(paperId.id, EntityType.PAPER)
        throwIfPaperDoesNotExist(paperUuid)
        repo.createReadingListEntry(currentUser.id, paperUuid)
        return Base.Nothing.newBuilder().build()
    }

    override suspend fun removePaperFromReadingList(paperId: Base.Id): Base.Nothing {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val paperUuid = parseUUID(paperId.id, EntityType.PAPER)
        throwIfPaperDoesNotExist(paperUuid)
        repo.removeReadingListEntry(currentUser.id, paperUuid)
        return Base.Nothing.newBuilder().build()
    }
}
