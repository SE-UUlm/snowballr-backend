package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.paper.PaperField
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import java.util.UUID

private val logger = KotlinLogging.logger {}

interface IPaperService {
    /**
     * Service implementation of [SnowballRService.getPaperById].
     */
    suspend fun getPaperById(paperId: UUID): PaperResponse

    /**
     * Service implementation of [SnowballRService.getBackwardReferencedPapers].
     */
    suspend fun getBackwardReferencedPapers(paperId: UUID): List<PaperResponse>

    /**
     * Service implementation of [SnowballRService.getForwardReferencedPapers].
     */
    suspend fun getForwardReferencedPapers(paperId: UUID): List<PaperResponse>

    /**
     * Service implementation of [SnowballRService.updatePaper].
     */
    suspend fun updatePaper(request: UpdatePaperRequest, paths: List<PaperField>): PaperResponse

    /**
     * Service implementation of [SnowballRService.createPaper].
     */
    suspend fun createPaper(request: CreatePaperRequest): PaperResponse
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
    override suspend fun getPaperById(paperId: UUID) = repo.getPaperById(paperId).getOrThrow().toPaperResponse()

    override suspend fun getBackwardReferencedPapers(paperId: UUID): List<PaperResponse> =
        getReferencePapers(paperId, citationRepo::getBackwardsReferencedPaperIdsOfPaperById)

    override suspend fun getForwardReferencedPapers(paperId: UUID): List<PaperResponse> =
        getReferencePapers(paperId, citationRepo::getForwardReferencedPaperIdsOfPaperById)

    override suspend fun updatePaper(request: UpdatePaperRequest, paths: List<PaperField>): PaperResponse {
        repo.ensurePaperExists(request.paperId)

        val isExternalIdChange = paths.contains(PaperField.EXTERNAL_IDS) && request.externalIds.isNotEmpty()
        if (isExternalIdChange) {
            val existingPapers = repo.getPapersByExternalIds(request.externalIds)

            if (existingPapers.any { it.id != request.paperId }) {
                throw DuplicatePaperException(request.externalIds)
            }
        }

        val updatedPaper = repo.updatePaper(request, paths)
        logger.info { "Paper ${request.paperId} updated: ${paths.joinToString()}" }
        return updatedPaper.toPaperResponse()
    }

    override suspend fun createPaper(request: CreatePaperRequest): PaperResponse {
        if (request.externalIds.isNotEmpty() && repo.doesPaperExistByExternalIds(request.externalIds)) {
            throw DuplicatePaperException(request.externalIds)
        }

        val paper = repo.createPaper(request)
        logger.info { "Paper ${paper.id} created ('${paper.title}')" }
        return paper.toPaperResponse()
    }

    /**
     * Retrieves a list of reference papers based on the provided paper ID and a specified function for fetching
     * references. This method ensures the validity of the paper ID and retrieves the associated metadata for each
     * reference paper, including backward references.
     *
     * @param paperId The ID of the paper for which references are to be retrieved.
     * @param function A function that takes a paper ID and returns a list of UUIDs of the references.
     * @return A list of paper objects containing reference information.
     * @throws NotFoundException If the paper specified in the request does not exist.
     */
    private suspend fun getReferencePapers(paperId: UUID, function: suspend (UUID) -> List<UUID>): List<PaperResponse> {
        repo.ensurePaperExists(paperId)

        val referenceIds = function.invoke(paperId)
        val papers = referenceIds.map { getPaperById(it) }

        return papers
    }

    private suspend fun Paper.toPaperResponse(): PaperResponse = this.toPaperResponse(citationRepo)
}
