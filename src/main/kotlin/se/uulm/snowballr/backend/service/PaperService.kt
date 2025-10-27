package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toGrpcPapers
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IPdfTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

interface IPaperService {
    /**
     * Service implementation of [SnowballRService.getPaperById].
     */
    suspend fun getPaperById(paperId: UUID): GrpcPaper

    /**
     * Service implementation of [SnowballRService.getBackwardReferencedPapers].
     */
    suspend fun getBackwardReferencedPapers(paperId: UUID): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.getForwardReferencedPapers].
     */
    suspend fun getForwardReferencedPapers(paperId: UUID): GrpcPaper.List

    /**
     * Service implementation of [SnowballRService.updatePaper].
     */
    suspend fun updatePaper(request: GrpcPaper.Update): GrpcPaper

    /**
     * Service implementation of [SnowballRService.createPaper].
     */
    suspend fun createPaper(request: GrpcPaper): GrpcPaper

    /**
     * Service implementation of [SnowballRService.setPaperPdf].
     */
    suspend fun setPaperPdf(request: GrpcPaper.PdfUpdate): Base.Nothing

    /**
     * Service implementation of [SnowballRService.getPaperPdf].
     */
    suspend fun getPaperPdf(request: Base.Id): Base.Blob
}

/**
 * The [PaperService] class handles operations related to normal papers by implementing the [IPaperService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of paper CRUD operations,
 * delegating the actual persistence operations to the [IPaperTableRepo] repository.
 *
 * @constructor Initializes the [PaperService] with a paper repository.
 * @param repo The repository responsible for managing persistence operations for normal papers.
 * @param pdfRepo The repository responsible for managing persistence operations for PDFs.
 * @param citationRepo The repository responsible for managing persistence operations for paper citations.
 */
class PaperService(
    private val repo: IPaperTableRepo,
    private val pdfRepo: IPdfTableRepo,
    private val citationRepo: ICitationTableRepo,
) : IPaperService {
    override suspend fun getPaperById(paperId: UUID): GrpcPaper {
        val paper = repo.getPaperById(paperId).getOrThrow()

        return paper.toGrpcPaper()
    }

    override suspend fun getBackwardReferencedPapers(paperId: UUID): GrpcPaper.List =
        getReferencePapers(paperId, citationRepo::getBackwardsReferencedPaperIdsOfPaperById)

    override suspend fun getForwardReferencedPapers(paperId: UUID): GrpcPaper.List =
        getReferencePapers(paperId, citationRepo::getForwardReferencedPaperIdsOfPaperById)

    override suspend fun updatePaper(request: GrpcPaper.Update): GrpcPaper {
        val paperId = parseUUID(request.paper.id, EntityType.PAPER)

        repo.ensurePaperExists(paperId)

        if (request.paper.externalId.isNotEmpty()) {
            val existingPaper = repo.getPaperByExternalId(request.paper.externalId).getOrNull()
            if (existingPaper != null && existingPaper.id != paperId) {
                throw DuplicatePaperException(request.paper.externalId)
            }
        }

        return repo.updatePaper(request).toGrpcPaper()
    }

    override suspend fun createPaper(request: GrpcPaper): GrpcPaper {
        if (request.externalId.isNotEmpty() && repo.doesPaperExistByExternalId(request.externalId)) {
            throw DuplicatePaperException(request.externalId)
        }

        return repo.createPaper(request).toGrpcPaper()
    }

    /**
     * Retrieves a list of reference papers based on the provided paper ID and a specified function for fetching
     * references. This method ensures the validity of the paper ID and retrieves the associated metadata for each
     * reference paper, including authors and backward references.
     *
     * @param paperId The ID of the paper for which references are to be retrieved.
     * @param function A function that takes a paper ID and returns a list of UUIDs of the references.
     * @return A list of gRPC-compatible paper objects containing reference information.
     * @throws NotFoundException If the paper specified in the request does not exist.
     */
    private suspend fun getReferencePapers(paperId: UUID, function: suspend (UUID) -> List<UUID>): GrpcPaper.List {
        repo.ensurePaperExists(paperId)

        val referenceIds = function.invoke(paperId)
        val papers = referenceIds.map {
            val referencedPaper = repo.getPaperById(it).getOrThrow()
            referencedPaper.toGrpcPaper()
        }
        return papers.toGrpcPapers()
    }

    private suspend fun Paper.toGrpcPaper(): GrpcPaper = this.toGrpcPaperWithAuthorsAndBackwardReferences(citationRepo)

    override suspend fun setPaperPdf(request: GrpcPaper.PdfUpdate): Base.Nothing {
        val paperId = parseUUID(request.paperId, EntityType.PAPER)

        // Verify paper exists
        if (!repo.doesPaperExistById(paperId)) {
            throw NotFoundException(EntityType.PAPER, paperId.toString())
        }

        // Get current paper to check if it has an existing PDF
        val paper = repo.getPaperById(paperId).getOrThrow()

        // Delete old PDF if it exists
        if (paper.pdfId != null) {
            pdfRepo.deletePdfById(paper.pdfId)
        }

        // If new PDF data is provided, create it and update the paper
        if (request.hasPdf() && request.pdf.data.size() > 0) {
            val newPdf = pdfRepo.createPdf(request.pdf.data.toByteArray())
            repo.updatePaperPdfId(paperId, newPdf.id)
        } else {
            // Remove PDF reference from paper
            repo.updatePaperPdfId(paperId, null)
        }

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun getPaperPdf(request: Base.Id): Base.Blob {
        val paperId = parseUUID(request.id, EntityType.PAPER)

        // Verify paper exists
        val paper = repo.getPaperById(paperId).getOrElse {
            throw NotFoundException(EntityType.PAPER, paperId.toString())
        }

        // Check if paper has a PDF
        if (paper.pdfId == null) {
            throw FailedPreconditionException("Paper does not have an attached PDF")
        }

        // Get the PDF
        val pdf = pdfRepo.getPdfById(paper.pdfId).getOrElse {
            throw NotFoundException(EntityType.PDF, paper.pdfId.toString())
        }

        // Return the PDF as a Blob
        return Base.Blob.newBuilder()
            .setData(com.google.protobuf.ByteString.copyFrom(pdf.data))
            .build()
    }
}
