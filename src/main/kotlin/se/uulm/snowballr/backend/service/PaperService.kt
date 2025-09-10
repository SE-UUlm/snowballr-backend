package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.model.dto.toGrpcPapers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import snowballr.Base
import snowballr.PaperOuterClass
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
 */
class PaperService(
    private val repo: IPaperTableRepo,
    private val authorOfPapersRepo: IAuthorOfPaperTableRepo,
    private val citationRepo: ICitationTableRepo,
) : IPaperService {
    override suspend fun getPaperById(request: Base.Id): GrpcPaper {
        val paperId = parseUUID(request.id, EntityType.PAPER)
        val paper = repo.getPaperById(paperId)

        val authors = authorOfPapersRepo.getAuthorsOfPaperById(paperId).map { it.toGrpcAuthor() }
        val backwardReferencedIds = citationRepo.getBackwardsReferencedPaperIdsOfPaperById(paperId)
            .map { it.toString() }
        return paper.toGrpcPaper(authors, backwardReferencedIds)
    }

    override suspend fun getBackwardReferencedPapers(request: Base.Id): GrpcPaper.List {
        val paperId = parseUUID(request.id, EntityType.PAPER)
        if (!repo.doesPaperExistById(paperId)) {
            throw SnowballRException.NotFoundException(EntityType.PAPER, paperId.toString())
        }

        val backwardReferencedIds = citationRepo.getBackwardsReferencedPaperIdsOfPaperById(paperId)
        val papers: MutableList<Paper> = mutableListOf()
        val paperAuthorsMap = mutableMapOf<Paper, List<PaperOuterClass.Author>>()
        val paperBackwardReferencesMap = mutableMapOf<Paper, List<String>>()
        backwardReferencedIds.forEach {
            val referencedPaper = repo.getPaperById(it)
            papers.add(referencedPaper)
            paperAuthorsMap[referencedPaper] = authorOfPapersRepo
                .getAuthorsOfPaperById(referencedPaper.id).map { author -> author.toGrpcAuthor() }
            paperBackwardReferencesMap[referencedPaper] = citationRepo
                .getBackwardsReferencedPaperIdsOfPaperById(referencedPaper.id).map { reference ->
                    reference.toString()
                }
        }

        return papers.toGrpcPapers(paperAuthorsMap, paperBackwardReferencesMap)
    }
}
