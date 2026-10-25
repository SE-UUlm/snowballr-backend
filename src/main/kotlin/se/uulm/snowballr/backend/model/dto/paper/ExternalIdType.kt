package se.uulm.snowballr.backend.model.dto.paper

/**
 * The type of an [ExternalId].
 *
 * @property displayName The displayable name of the type.
 * @property isCaseInsensitive Whether IDs of this type are case-insensitive, i.e., values that only differ in casing
 * refer to the same entity. Values of such types are lowercased by [ExternalId.normalized].
 */
enum class ExternalIdType(val displayName: String, val isCaseInsensitive: Boolean = false) {
    /**
     * [Digital Object Identifier](https://www.doi.org/). DOIs are case-insensitive, see the
     * [DOI Handbook](https://www.doi.org/doi-handbook/html/index.html), chapter 3.3.4.
     */
    DOI("DOI", isCaseInsensitive = true),

    /**
     * [arXiv](https://arxiv.org/).
     */
    ARXIV("ArXiv"),

    /**
     * [Microsoft Academic Graph](https://www.microsoft.com/en-us/research/project/microsoft-academic-graph/).
     */
    MAG("MAG"),

    /**
     * [Association of Computational Linguistics](https://www.aclweb.org/anthology/).
     */
    ACL("ACL"),

    /**
     * [PubMed](https://pubmed.ncbi.nlm.nih.gov/).
     */
    PUB_MED("PubMed"),

    /**
     * [Medline](https://www.nlm.nih.gov/medline/medline_home.html).
     */
    MEDLINE("Medline"),

    /**
     * [PubMed Central](https://pmc.ncbi.nlm.nih.gov/).
     */
    PUB_MED_CENTRAL("PubMed Central"),

    /**
     * [dblp](https://dblp.org/).
     */
    DBLP("DBLP"),

    /**
     * [Semantic Scholar](https://www.semanticscholar.org/).
     */
    SEMANTIC_SCHOLAR("Semantic Scholar"),
}
