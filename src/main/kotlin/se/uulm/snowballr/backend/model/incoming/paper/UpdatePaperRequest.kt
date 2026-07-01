package se.uulm.snowballr.backend.model.incoming.paper

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import java.util.UUID

data class UpdatePaperRequest(
    val paperId: UUID,
    val title: String,
    val externalId: String?,
    val abstract: String,
    val year: Int,
    val publisher: String,
    val publicationName: String,
    val publicationType: String,
    val authors: List<Author>,
) {
    companion object {
        fun fromPaper(paper: Paper) = UpdatePaperRequest(
            paperId = paper.id,
            title = paper.title,
            externalId = paper.externalId,
            abstract = paper.abstract,
            year = paper.year,
            publisher = paper.publisher,
            publicationName = paper.publicationName,
            publicationType = paper.publicationType,
            authors = paper.authors,
        )

        fun fromPaperResponse(paper: PaperResponse) = UpdatePaperRequest(
            paperId = paper.id,
            title = paper.title,
            externalId = paper.externalId,
            abstract = paper.abstract,
            year = paper.year,
            publisher = paper.publisher,
            publicationName = paper.publicationName,
            publicationType = paper.publicationType,
            authors = paper.authors,
        )
    }
}
