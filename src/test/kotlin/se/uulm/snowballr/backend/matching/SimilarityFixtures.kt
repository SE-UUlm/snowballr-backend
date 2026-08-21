package se.uulm.snowballr.backend.matching

import org.junit.jupiter.params.provider.Arguments
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper

data class SimilarityFixture(
    val a: FetcherPaper,
    val b: FetcherPaper,
    val isSame: Boolean,
)

object SimilarityFixtures {
    @JvmStatic
    fun getRealSimilarityExamples() =
        listOf(
            SimilarityFixture(
                FetcherPaper(
                    title = "Theoretical and numerical constraint-handling techniques used with evolutionary algorithms: a survey of the state of the art",
                    externalIds = listOf(
                        ExternalId(
                            type = ExternalIdType.MAG,
                            value = "1535482498",
                        ),
                        ExternalId(
                            type = ExternalIdType.SEMANTIC_SCHOLAR,
                            value = "ec0434534d9e80ac984f061d0fef193f79d284ff",
                        ),
                    ),
                    abstract = "", year = 2002, publisher = "", publicationType = "Review", publicationName = "",
                    authors = listOf(
                        Author(
                            firstName = "C.",
                            lastName = "Coello - Coello",
                        ),
                    ),
                    fetcherMetadata = mapOf(
                        "SemanticScholarId" to "ec0434534d9e80ac984f061d0fef193f79d284ff",
                        "SemanticScholarCorpusId" to "9235579",
                    ),
                ),
                FetcherPaper(
                    title = "THEORETICAL AND NUMERICAL CONSTRAINT-HANDLING TECHNIQUES USED WITH EVOLUTIONARY ALGORITHMS: A SURVEY OF THE STATE OF THE ART",
                    externalIds = listOf(
                        ExternalId(
                            type = ExternalIdType.DOI,
                            value = "10.1016/S0045-7825(01)00323-1",
                        ),
                        ExternalId(
                            type = ExternalIdType.MAG,
                            value = "2167580870",
                        ),
                        ExternalId(
                            type = ExternalIdType.SEMANTIC_SCHOLAR,
                            value = "232fd0ff4c568edf8ad1a1220b362f492dd725a7",
                        ),
                    ),
                    abstract = "",
                    year = 2002,
                    publisher = "",
                    publicationType = "Review",
                    publicationName = "",
                    authors = listOf(Author(firstName = "C.", lastName = "Coello")),
                    fetcherMetadata = mapOf(
                        "SemanticScholarId" to "232fd0ff4c568edf8ad1a1220b362f492dd725a7",
                        "SemanticScholarCorpusId" to "62303805",
                    ),
                ),
                isSame = true,
            ),
        ).map { Arguments.of(it) }
}
