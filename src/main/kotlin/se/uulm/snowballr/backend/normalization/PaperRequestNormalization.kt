package se.uulm.snowballr.backend.normalization

import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest

/**
 * Normalizes user-submitted paper data the same way [PaperNormalizer] normalizes fetcher-sourced data, so that
 * manually entered papers stay consistent with fetched ones for deduplication and matching.
 */
fun CreatePaperRequest.normalized(): CreatePaperRequest = copy(
    title = PaperNormalizer.normalizeText(title),
    abstract = PaperNormalizer.normalizeText(abstract),
    publisher = PaperNormalizer.normalizeText(publisher),
    publicationType = PaperNormalizer.normalizeText(publicationType),
    publicationName = PaperNormalizer.normalizeText(publicationName),
    authors = PaperNormalizer.normalizeAuthors(authors),
    externalIds = PaperNormalizer.normalizeExternalIds(externalIds),
)

/**
 * Normalizes user-submitted paper data the same way [PaperNormalizer] normalizes fetcher-sourced data, so that
 * manually entered papers stay consistent with fetched ones for deduplication and matching.
 */
fun UpdatePaperRequest.normalized(): UpdatePaperRequest = copy(
    title = PaperNormalizer.normalizeText(title),
    abstract = PaperNormalizer.normalizeText(abstract),
    publisher = PaperNormalizer.normalizeText(publisher),
    publicationType = PaperNormalizer.normalizeText(publicationType),
    publicationName = PaperNormalizer.normalizeText(publicationName),
    authors = PaperNormalizer.normalizeAuthors(authors),
    externalIds = PaperNormalizer.normalizeExternalIds(externalIds),
)
