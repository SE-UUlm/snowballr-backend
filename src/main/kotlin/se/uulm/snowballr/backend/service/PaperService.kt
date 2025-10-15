package se.uulm.snowballr.backend.service

import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toGrpcPapers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IAuthorTableRepo
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import snowballr.Base
import java.util.UUID
import snowballr.PaperOuterClass.Author as GrpcAuthor
import snowballr.PaperOuterClass.Paper as GrpcPaper

interface IPaperService {
    /**
     * Service implementation of [SnowballRService.getPaperById].
     */
    suspend fun getPaperById(request: Base.Id): GrpcPaper

    /**
     * Service implementation of [SnowballRService.getBackwardReferencedPapers].
     */
    suspend fun getBackwardReferencedPapers(request: Base.Id): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.getForwardReferencedPapers].
     */
    suspend fun getForwardReferencedPapers(request: Base.Id): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.updatePaper].
     */
    suspend fun updatePaper(request: GrpcPaper.Update): GrpcPaper

    /**
     * Service implementation of [SnowballRService.createPaper].
     */
    suspend fun createPaper(request: GrpcPaper): GrpcPaper
}

/**
 * The [PaperService] class handles operations related to normal papers by implementing the [IPaperService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of paper CRUD operations,
 * delegating the actual persistence operations to the [IPaperTableRepo] repository.
 *
 * @constructor Initializes the [PaperService] with a paper repository.
 * @param repo The repository responsible for managing persistence operations for normal papers.
 * @param authorOfPapersRepo The repository responsible for managing persistence operations for author-paper associations.
 * @param citationRepo The repository responsible for managing persistence operations for paper citations.
 * @param authorRepo The repository responsible for managing persistence operations for authors.
 */
@Suppress("TooManyFunctions")
class PaperService(
    private val repo: IPaperTableRepo,
    private val authorOfPapersRepo: IAuthorOfPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
    private val authorRepo: IAuthorTableRepo,
) : IPaperService {
    override suspend fun getPaperById(request: Base.Id): GrpcPaper {
        val paperId = parseUUID(request.id, EntityType.PAPER)
        val paper = repo.getPaperById(paperId).getOrThrow()

        return paper.toGrpcPaper()
    }

    override suspend fun getBackwardReferencedPapers(request: Base.Id): GrpcPaper.List =
        getReferencePapers(request, citationRepo::getBackwardsReferencedPaperIdsOfPaperById)

    override suspend fun getForwardReferencedPapers(request: Base.Id): GrpcPaper.List =
        getReferencePapers(request, citationRepo::getForwardReferencedPaperIdsOfPaperById)

    override suspend fun updatePaper(request: GrpcPaper.Update): GrpcPaper {
        val paperId = parseUUID(request.paper.id, EntityType.PAPER)

        if (!repo.doesPaperExistById(paperId)) {
            throw NotFoundException(EntityType.PAPER, paperId.toString())
        }

        val fieldMask = FieldMaskUtil.normalize(request.mask)

        if (fieldMask.pathsList.contains("paper.authors")) {
            handleAuthorsChanges(request.paper, paperId)
        }

        return repo.updatePaper(request).toGrpcPaper()
    }

    override suspend fun createPaper(request: GrpcPaper): GrpcPaper = repo.createPaper(request).toGrpcPaper()

    /**
     * Retrieves a list of reference papers based on the provided paper ID and a specified function for fetching
     * references. This method ensures the validity of the paper ID and retrieves the associated metadata for each
     * reference paper, including authors and backward references.
     *
     * @param request The request containing the ID of the paper for which references are to be retrieved.
     * @param function A function that takes a paper ID and returns a list of UUIDs of the references.
     * @return A list of gRPC-compatible paper objects containing reference information.
     * @throws NotFoundException If the paper specified in the request does not exist.
     */
    private suspend fun getReferencePapers(request: Base.Id, function: suspend (UUID) -> List<UUID>): GrpcPaper.List {
        val paperId = parseUUID(request.id, EntityType.PAPER)
        if (!repo.doesPaperExistById(paperId)) {
            throw NotFoundException(EntityType.PAPER, paperId.toString())
        }

        val referenceIds = function.invoke(paperId)
        val papers = referenceIds.map {
            val referencedPaper = repo.getPaperById(it).getOrThrow()
            referencedPaper.toGrpcPaper()
        }
        return papers.toGrpcPapers()
    }

    private suspend fun Paper.toGrpcPaper(): GrpcPaper =
        this.toGrpcPaperWithAuthorsAndBackwardReferences(authorOfPapersRepo, citationRepo)

    private suspend fun handleAuthorsChanges(paper: GrpcPaper, paperId: UUID) {
        val newAuthors = paper.authorsList.toList()
        val existingAuthors = authorOfPapersRepo.getAuthorsOfPaperById(paperId)

        handleAuthorsRemoval(paperId, existingAuthors, newAuthors)
        handleAuthorsAdditionAndUpdate(paperId, existingAuthors, newAuthors)
    }

    private suspend fun handleAuthorsAdditionAndUpdate(
        paperId: UUID,
        existingAuthors: List<Author>,
        newAuthors: List<GrpcAuthor>,
    ) {
        val (authorsToAdd, authorsToUpdate) = getAuthorsToAddAndUpdate(existingAuthors, newAuthors)

        val addedAuthors = authorsToAdd.map { authorRepo.createAuthor(it) }
        for (author in addedAuthors) {
            authorOfPapersRepo.addAuthorToPaper(author.id, paperId)
        }

        for ((existingAuthor, newAuthor) in authorsToUpdate) {
            authorRepo.updateAuthor(existingAuthor.id, newAuthor)
        }
    }

    private fun getAuthorsToAddAndUpdate(
        existingAuthors: List<Author>,
        newAuthors: List<GrpcAuthor>,
    ): Pair<List<GrpcAuthor>, List<Pair<Author, GrpcAuthor>>> {
        val authorsToAdd = mutableListOf<GrpcAuthor>()
        val authorsToUpdate = mutableListOf<Pair<Author, GrpcAuthor>>()

        for (newAuthor in newAuthors) {
            val matchingAuthor = existingAuthors.find { areAuthorsEqual(it, newAuthor) }

            if (matchingAuthor != null) {
                authorsToUpdate.add(Pair(matchingAuthor, newAuthor))
            } else {
                authorsToAdd.add(newAuthor)
            }
        }

        return Pair(authorsToAdd, authorsToUpdate)
    }

    private suspend fun handleAuthorsRemoval(
        paperId: UUID,
        existingAuthors: List<Author>,
        newAuthors: List<GrpcAuthor>,
    ) {
        val authorsToRemove = getAuthorsToRemove(existingAuthors, newAuthors)
        for (author in authorsToRemove) {
            authorOfPapersRepo.removeAuthorFromPaper(author.id, paperId)
        }
    }

    private fun getAuthorsToRemove(existingAuthors: List<Author>, newAuthors: List<GrpcAuthor>): List<Author> =
        existingAuthors.filter { existingAuthor ->
            newAuthors.none { areAuthorsEqual(existingAuthor, it) }
        }

    private fun areAuthorsEqual(author: Author, grpcAuthor: GrpcAuthor) = author.orcid == grpcAuthor.orcid ||
        (author.firstName == grpcAuthor.firstName && author.lastName == grpcAuthor.lastName)
}
