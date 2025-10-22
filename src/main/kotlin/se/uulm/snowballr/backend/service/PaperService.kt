package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toGrpcPapers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import snowballr.Base
import java.util.UUID
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
 * @param citationRepo The repository responsible for managing persistence operations for paper citations.
 */
class PaperService(
    private val repo: IPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
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

        return repo.updatePaper(request).toGrpcPaper()
    }

    override suspend fun createPaper(request: GrpcPaper): GrpcPaper {
        if (repo.doesPaperExistByExternalId(request.externalId)) {
            throw DuplicateEntityException(EntityType.PAPER, request.externalId)
        }

        return repo.createPaper(request).toGrpcPaper()
    }

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

    private suspend fun Paper.toGrpcPaper(): GrpcPaper = this.toGrpcPaperWithAuthorsAndBackwardReferences(citationRepo)
}
