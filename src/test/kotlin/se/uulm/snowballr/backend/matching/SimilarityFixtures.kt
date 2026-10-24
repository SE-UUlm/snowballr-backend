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
            SimilarityFixture(
                FetcherPaper(
                    title = "Tabu Search - Part I",
                    externalIds = listOf(
                        ExternalId(
                            type = ExternalIdType.DOI,
                            value = "10.1287/ijoc.1.3.190",
                        ),
                        ExternalId(
                            type = ExternalIdType.DBLP,
                            value = "journals/informs/Glover89",
                        ),
                        ExternalId(
                            type = ExternalIdType.SEMANTIC_SCHOLAR,
                            value = "d115adcbf63e989804912db1e656ec8debd1f040",
                        ),
                    ),
                    abstract = "",
                    year = 1989,
                    publisher = "",
                    publicationType = "JournalArticle",
                    publicationName = "INFORMS journal on computing",
                    authors = listOf(Author(firstName = "F.", lastName = "Glover")),
                    fetcherMetadata = mapOf(
                        "SemanticScholarId" to "d115adcbf63e989804912db1e656ec8debd1f040",
                        "SemanticScholarCorpusId" to "5617719",
                    ),
                ),
                FetcherPaper(
                    title = "Tabu Search - Part II",
                    externalIds = listOf(
                        ExternalId(
                            type = ExternalIdType.DOI,
                            value = "10.1287/IJOC.2.1.4",
                        ),
                        ExternalId(
                            type = ExternalIdType.MAG,
                            value = "2104670598",
                        ),
                        ExternalId(
                            type = ExternalIdType.DBLP,
                            value = "journals/informs/Glover90",
                        ),
                        ExternalId(
                            type = ExternalIdType.SEMANTIC_SCHOLAR,
                            value = "9a3203a26112ec33d34233ba8b38e5509490f2a0",
                        ),
                    ),
                    abstract = "",
                    year = 1989,
                    publisher = "",
                    publicationType = "JournalArticle",
                    publicationName = "INFORMS journal on computing",
                    authors = listOf(Author(firstName = "F.", lastName = "Glover")),
                    fetcherMetadata = mapOf(
                        "SemanticScholarId" to "9a3203a26112ec33d34233ba8b38e5509490f2a0",
                    ),
                ),
                isSame = false,
            ),
            SimilarityFixture(
                FetcherPaper(
                    title = "Optimization by Simmulated Annealing",
                    externalIds = listOf(
                        ExternalId(
                            type = ExternalIdType.MAG,
                            value = "3022584652",
                        ),
                        ExternalId(
                            type = ExternalIdType.DBLP,
                            value = "journals/science/KirkpatrickGV83",
                        ),
                        ExternalId(
                            type = ExternalIdType.SEMANTIC_SCHOLAR,
                            value = "0a0c026b2b6c04baaf1fa2933d5998519bc9c5fa",
                        ),
                    ),
                    abstract = "",
                    year = 1983,
                    publisher = "",
                    publicationType = "JournalArticle",
                    publicationName = "The Scientist",
                    authors = listOf(
                        Author(firstName = "S.", lastName = "Kirkpatrick"),
                        Author(firstName = "D.", lastName = "Gelatt"),
                        Author(firstName = "M.", lastName = "Vecchi"),
                    ),
                    fetcherMetadata = mapOf(
                        "SemanticScholarId" to "0a0c026b2b6c04baaf1fa2933d5998519bc9c5fa",
                        "SemanticScholarCorpusId" to "40427348",
                    ),
                ),
                FetcherPaper(
                    title = "Optimization by Simulated Annealing",
                    externalIds = listOf(
                        ExternalId(
                            type = ExternalIdType.DOI,
                            value = "10.1126/science.220.4598.671",
                        ),
                        ExternalId(
                            type = ExternalIdType.MAG,
                            value = "3042865397",
                        ),
                        ExternalId(
                            type = ExternalIdType.PUB_MED,
                            value = "17813860",
                        ),
                        ExternalId(
                            type = ExternalIdType.SEMANTIC_SCHOLAR,
                            value = "dd5061631a4d11fa394f4421700ebf7e78dcbc59",
                        ),
                    ),
                    abstract = "",
                    year = 1983,
                    publisher = "",
                    publicationType = "JournalArticle",
                    publicationName = "Science",
                    authors = listOf(
                        Author(firstName = "S.", lastName = "Kirkpatrick"),
                        Author(firstName = "C. D.", lastName = "Gelatt"),
                        Author(firstName = "M.", lastName = "Vecchi"),
                    ),
                    fetcherMetadata = mapOf(
                        "SemanticScholarId" to "dd5061631a4d11fa394f4421700ebf7e78dcbc59",
                        "SemanticScholarCorpusId" to "205939",
                    ),
                ),
                isSame = true,
            ),
        ).map { Arguments.of(it) }
}
