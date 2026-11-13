package se.uulm.snowballr.backend.fetcher

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
import se.uulm.snowballr.backend.model.fetcher.FetcherInformation
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherOptionsSchema
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import kotlin.random.Random

class DummyFetcherManager(
    private val random: Random = Random.Default,
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
                        defaultValue = null,
                    ),
                    "option2" to FetcherOptionsSchema(
                        name = "Option 2",
                        description = "Another dummy option for testing purposes",
                        isRequired = true,
                        isSecret = true,
                        defaultValue = "Tomato",
                    ),
                ),
            ),
        ),
        FetcherInformationWithId(
            id = "d-2",
            information = FetcherInformation(
                name = "Dummy Fetcher 2",
                description = "Another dummy fetcher for testing purposes",
                links = emptyList(),
                optionsSchema = emptyMap(),
            ),
        ),
    )

    private val dummyPapers = listOf(
        FetcherPaper(
            title = "Exploring the Universe of AI",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.1234/abcd.5678")),
            abstract = "Artificial Intelligence (AI) has been a subject of research for decades. It has revolutionized " +
                    "multiple fields, from healthcare to finance. However, there are still significant challenges in " +
                    "terms of ethics and implementation. AI systems require vast datasets to train, and biases in " +
                    "those datasets can lead to unfair results. The future of AI promises more intuitive machines, " +
                    "but there is a need for further exploration of its social impacts. With advancements in machine " +
                    "learning, AI continues to evolve. Yet, the boundaries of what AI can achieve remain unclear. In " +
                    "this paper, we discuss both the current state and future possibilities of AI technologies. We " +
                    "also provide a roadmap for overcoming its limitations.",
            year = 2014,
            publisher = "Springer",
            publicationType = "Journal Article",
            publicationName = "Journal of Artificial Intelligence Research",
            authors = listOf(
                Author(firstName = "Henry", lastName = "Garcia"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "The Role of Data in Modern Research",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.2345/xyz.9876")),
            abstract = "In modern research, data plays a crucial role in driving discoveries. The availability of vast " +
                    "amounts of data has changed how research is conducted across many domains. However, data alone " +
                    "is not enough; it must be interpreted correctly. Researchers need to be equipped with tools to " +
                    "handle large datasets, such as machine learning algorithms and statistical models. This paper " +
                    "explores how data science has impacted various research fields, including biology, physics, and " +
                    "economics. The challenges of data privacy and security are also discussed. Furthermore, the " +
                    "paper highlights the ethical considerations in data collection and usage. The future of research" +
                    " relies heavily on how data is handled and analyzed.",
            year = 2019,
            publisher = "Elsevier",
            publicationType = "Research Report",
            publicationName = "Journal of Data Science",
            authors = listOf(
                Author(firstName = "David", lastName = "Brown"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Artificial Intelligence in Healthcare",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.5432/lmn.1234")),
            abstract = "The application of AI in healthcare has seen significant growth in recent years. From diagnostic" +
                    " tools to personalized treatment plans, AI is revolutionizing medical practices. However, " +
                    "implementing AI solutions in healthcare presents unique challenges. Data privacy and security " +
                    "remain top concerns, especially given the sensitive nature of medical information. Moreover, the" +
                    " integration of AI into existing healthcare systems requires substantial investment. Despite " +
                    "these hurdles, the potential to improve patient outcomes is immense. AI-powered devices can " +
                    "analyze medical images with remarkable accuracy. In this paper, we explore the state of AI in " +
                    "healthcare and the barriers to its full integration.",
            year = 2014,
            publisher = "Wiley",
            publicationType = "Journal Article",
            publicationName = "AI in Healthcare Journal",
            authors = listOf(
                Author(firstName = "Quincy", lastName = "Moore"),
                Author(firstName = "Bob", lastName = "Johnson"),
                Author(firstName = "Mia", lastName = "Wilson"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Blockchain Technology: A New Era of Security",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.6789/pqr.4321")),
            abstract = "Blockchain technology has emerged as a revolutionary way to secure digital transactions. It " +
                    "ensures data integrity through decentralized systems and cryptographic algorithms. The promise " +
                    "of blockchain goes beyond cryptocurrencies, with applications in supply chain management, " +
                    "healthcare, and voting systems. However, the technology is not without its challenges. Issues " +
                    "such as scalability, energy consumption, and regulatory concerns need to be addressed. In this " +
                    "paper, we review the evolution of blockchain technology and its current applications. We also " +
                    "discuss the potential for blockchain to disrupt traditional industries and the barriers to its " +
                    "widespread adoption. Despite the challenges, blockchain remains a promising technology with " +
                    "wide-reaching implications.",
            year = 1999,
            publisher = "IEEE",
            publicationType = "Conference Paper",
            publicationName = "Journal of Blockchain Technology",
            authors = listOf(
                Author(firstName = "Bob", lastName = "Johnson"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Advances in Deep Learning",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.1111/abc.1234")),
            abstract = "Deep learning has emerged as one of the most influential techniques in machine learning. With " +
                    "the development of sophisticated neural networks, deep learning has revolutionized fields like " +
                    "computer vision, natural language processing, and speech recognition. This paper explores recent" +
                    " advancements in deep learning architectures, including convolutional and recurrent neural " +
                    "networks. It also discusses the applications of deep learning in various industries, along with " +
                    "the challenges that remain in terms of data availability, training costs, and model " +
                    "interpretability.",
            year = 2019,
            publisher = "MIT Press",
            publicationType = "Book",
            publicationName = "Journal of Machine Learning Research",
            authors = listOf(
                Author(firstName = "Paul", lastName = "Taylor"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Cybersecurity in the Age of IoT",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.3457/xyz.9876")),
            abstract = "The rapid growth of the Internet of Things (IoT) has created new opportunities for connectivity," +
                    " but it also presents significant cybersecurity challenges. This paper explores the security " +
                    "risks posed by the proliferation of connected devices and offers recommendations for improving " +
                    "IoT security. Topics discussed include encryption, device authentication, and secure data " +
                    "transmission protocols. The paper emphasizes the importance of collaboration between industries," +
                    " governments, and consumers to safeguard the growing network of connected devices.",
            year = 1999,
            publisher = "ACM",
            publicationType = "White Paper",
            publicationName = "Journal of Cybersecurity",
            authors = listOf(
                Author(firstName = "Frank", lastName = "Miller"),
                Author(firstName = "Quincy", lastName = "Moore"),
                Author(firstName = "Mia", lastName = "Wilson"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Augmented Reality: Shaping the Future of Education",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.2345/abc.1234")),
            abstract = "Augmented reality (AR) is poised to revolutionize the educational landscape by offering " +
                    "immersive and interactive learning experiences. This paper investigates the potential " +
                    "applications of AR in classrooms, such as virtual field trips, 3D models of complex concepts, " +
                    "and real-time collaboration. The paper also discusses the challenges of implementing AR in " +
                    "education, including cost, infrastructure, and teacher training.",
            year = 2019,
            publisher = "Taylor & Francis",
            publicationType = "Magazine Article",
            publicationName = "Journal of Educational Technology",
            authors = listOf(
                Author(firstName = "Rachel", lastName = "Jackson"),
                Author(firstName = "Henry", lastName = "Garcia"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Neuroscience and the Human Brain",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.4568/xyz.4567")),
            abstract = "Recent advancements in neuroscience have provided deeper insights into the human brain and its " +
                    "functioning. This paper reviews current research on brain-computer interfaces, neural " +
                    "plasticity, and cognitive enhancement. It explores the implications of these advancements for " +
                    "fields such as medicine, psychology, and artificial intelligence. The paper also highlights the " +
                    "ethical considerations of manipulating the human brain through technology.",
            year = 1999,
            publisher = "Oxford University Press",
            publicationType = "Thesis/Dissertation",
            publicationName = "Journal of Neuroscience",
            authors = listOf(
                Author(firstName = "Mia", lastName = "Wilson"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "The Ethics of Gene Editing",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.6547/xyz.2345")),
            abstract = "Gene editing technologies, such as CRISPR, have made it possible to manipulate the DNA of living" +
                    " organisms with unprecedented precision. This paper explores the ethical implications of gene " +
                    "editing, including its potential to eliminate genetic diseases, its use in enhancing human " +
                    "traits, and the possibility of creating 'designer babies'. The paper also discusses the " +
                    "regulatory frameworks needed to ensure the responsible use of gene editing technologies.",
            year = 2021,
            publisher = "Cambridge University Press",
            publicationType = "Online Article",
            publicationName = "Ethics in Biotechnology",
            authors = listOf(
                Author(firstName = "David", lastName = "Brown"),
                Author(firstName = "Frank", lastName = "Miller"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "Biotechnology in Environmental Protection",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.1122/xyz.5678")),
            abstract = "Biotechnology has significant potential in addressing environmental challenges, including " +
                    "pollution control, waste management, and conservation efforts. This paper explores the various " +
                    "applications of biotechnology in environmental protection, such as the use of microbes to clean " +
                    "up oil spills, the development of biofuels, and the restoration of damaged ecosystems. The paper" +
                    " also examines the future potential of biotechnology in environmental sustainability.",
            year = 2021,
            publisher = "CRC Press",
            publicationType = "Newspaper Article",
            publicationName = "Environmental Biotechnology Journal",
            authors = listOf(
                Author(firstName = "Bob", lastName = "Johnson"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "The Rise of Virtual Reality in Entertainment",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.9875/abc.6543")),
            abstract = "Virtual reality (VR) is transforming the entertainment industry, offering immersive experiences " +
                    "in gaming, movies, and theme parks. This paper explores the growth of VR technology and its " +
                    "impact on how people consume entertainment. It also discusses the challenges of VR adoption, " +
                    "such as hardware costs, content creation, and user experience design.",
            year = 2022,
            publisher = "IEEE",
            publicationType = "Blog Post",
            publicationName = "Entertainment Technology Review",
            authors = listOf(
                Author(firstName = "Kathy", lastName = "Lopez"),
            ),
            fetcherMetadata = emptyMap(),
        ),
        FetcherPaper(
            title = "The Evolution of E-Commerce in the Digital Age",
            externalIds = listOf(ExternalId(type = ExternalIdType.DOI, value = "10.4312/abc.2111")),
            abstract = "E-commerce has grown rapidly with the advent of digital technologies, transforming the way " +
                    "businesses and consumers interact. This paper reviews the evolution of e-commerce, from " +
                    "traditional online shopping to the rise of mobile commerce, social commerce, and personalized " +
                    "shopping experiences. The paper also discusses the future of e-commerce, including the role of " +
                    "artificial intelligence, virtual reality, and blockchain in shaping the online shopping " +
                    "experience.",
            year = 2021,
            publisher = "Springer",
            publicationType = "Conference Paper",
            publicationName = "E-Commerce Trends Journal",
            authors = listOf(
                Author(firstName = "Henry", lastName = "Garcia"),
                Author(firstName = "Alice", lastName = "Smith"),
                Author(firstName = "Noah", lastName = "Anderson"),
            ),
            fetcherMetadata = emptyMap(),
        ),
    )

    override suspend fun getAvailableFetchers(): Set<FetcherInformationWithId> = dummyFetchers

    override suspend fun searchPapers(
        fetcher: String,
        searchQuery: String,
        options: Map<String, String>,
    ): Set<FetcherPaper> {
        requireFetcherExists(fetcher)

        return dummyPapers
            .filter { it.matchesQuery(searchQuery) }
            .map { it.copy(fetcherMetadata = mapOf("fetcherId" to fetcher)) }
            .shuffled(random)
            .toSet()
    }

    private fun FetcherPaper.matchesQuery(query: String): Boolean {
        val searchTerm = query.trim().lowercase()
        if (searchTerm == "paper") return true

        val attributes = buildList {
            add(title)
            add(abstract)
            add(publicationName)
            add(publisher)
            add(publicationType)
            authors.mapTo(this) { "${it.firstName} ${it.lastName}" }
            externalIds.mapTo(this) { it.value }
        }

        return attributes.any { it.lowercase().contains(searchTerm) }
    }

    override suspend fun fetchForwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper> {
        requireFetcherExists(fetcher)

        return emptySet()
    }

    override suspend fun fetchBackwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper> {
        requireFetcherExists(fetcher)

        return emptySet()
    }

    private fun requireFetcherExists(fetcher: String) {
        if (dummyFetchers.none { it.id == fetcher }) throw FetcherNotFoundException(fetcher)
    }
}
