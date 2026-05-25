package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import snowballr.paper
import snowballr.PaperOuterClass.Paper as GrpcPaper

interface PaperData {
    val title: String
    val externalId: String?
    val abstract: String
    val year: Int
    val publisher: String
    val publicationType: String
    val publicationName: String
    val authors: List<Author>
    val fetcherMetadata: FetcherMetadata
}

/**
 * Creates a [GrpcPaper] request from this [PaperData].
 */
fun PaperData.toGrpcPaperRequest(): GrpcPaper {
    val paper = this
    return paper {
        title = paper.title
        val paperExternalId = paper.externalId
        if (paperExternalId != null) externalId = paperExternalId
        abstrakt = paper.abstract
        year = paper.year
        publisher = paper.publisher
        publicationType = paper.publicationType
        publicationName = paper.publicationName
        authors.addAll(paper.authors.toGrpcAuthors())
        fetcherMetadata.putAll(paper.fetcherMetadata)
    }
}
