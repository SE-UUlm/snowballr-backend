package se.uulm.snowballr.backend.fetcher

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.fetcher.FetcherInformation
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherOptionsSchema
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import kotlin.random.Random

class DummyFetcherManager(
    private val random: Random?
) : IFetcherManager {
    private val dummyFetchers = setOf(
        FetcherInformationWithId(
            id = "d-1",
            information = FetcherInformation(
                name = "Dummy Fetcher 1",
                description = "A dummy fetcher for testing purposes",
                links = emptyList(),
                optionsSchema = mapOf(
                    "option1" to FetcherOptionsSchema(
                        name = "Option 1",
                        description = "A dummy option for testing purposes",
                        isRequired = false,
                        isSecret = false,
                        defaultValue = null
                    ),
                    "option2" to FetcherOptionsSchema(
                        name = "Option 2",
                        description = "Another dummy option for testing purposes",
                        isRequired = true,
                        isSecret = true,
                        defaultValue = "Tomato"
                    )
                )
            )
        ),
        FetcherInformationWithId(
            id = "d-2",
            information = FetcherInformation(
                name = "Dummy Fetcher 2",
                description = "Another dummy fetcher for testing purposes",
                links = emptyList(),
                optionsSchema = mapOf()
            )
        )
    )

    private val dummyPaperTemplates = setOf(
        "A Survey of %s",
        "%s: A Systematic Literature Review",
        "Towards a Better Understanding of %s",
        "An Empirical Study on %s",
        "Rethinking %s in Practice"
    )


    private val dummyPaperAuthors = setOf(
        Author(firstName = "Ada", lastName = "Lovelace"),
        Author(firstName = "Alan", lastName = "Turing"),
        Author(firstName = "Grace", lastName = "Hopper"),
        Author(firstName = "Edsger", lastName = "Dijkstra"),
        Author(firstName = "Barbara", lastName = "Liskov")
    )


    override suspend fun getAvailableFetchers(): Set<FetcherInformationWithId> = dummyFetchers

    override suspend fun searchPapers(
        fetcher: String,
        searchQuery: String,
        options: Map<String, String>
    ): Set<FetcherPaper> {
        return dummyPaperTemplates.shuffled(random ?: Random.Default)
            .mapIndexed { index, template -> createDummyPapers(template, index, fetcher, searchQuery) }
            .toSet()
    }

    private fun createDummyPapers(template: String, index: Int, fetcher: String, searchQuery: String): FetcherPaper =
        FetcherPaper(
            title = template.replace($$"$QUERY", searchQuery.trim()),
            externalIds = listOf(
                ExternalId(
                    type = ExternalIdType.DOI,
                    value = "10.9999/$fetcher.$index",
                )
            ),
            abstract = "This paper was found by the $fetcher fetcher searching for \"$searchQuery\". It does not exist in the local database yet.",
            year = 2020 + (index % 5),
            publisher = fetcher,
            publicationType = "Conference Paper",
            publicationName = "Proceedings of $fetcher",
            authors = listOf(
                dummyPaperAuthors.elementAt(index % dummyPaperAuthors.size),
                dummyPaperAuthors.elementAt((index + 1) % dummyPaperAuthors.size)
            ),
            fetcherMetadata = mapOf("fetcherId" to fetcher)
        )

    override suspend fun fetchForwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>
    ): Set<FetcherPaper> {
        return emptySet()
    }

    override suspend fun fetchBackwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>
    ): Set<FetcherPaper> {
        return emptySet()
    }
}
