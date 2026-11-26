package se.uulm.snowballr.backend.model.dto.paper

/**
 * The type of an [ExternalId].
 *
 * @property displayName The displayable name of the type.
 * @property canonicalCase The casing that values of this type are converted to by [ExternalId.normalized]. Anything
 * other than [CanonicalCase.AS_IS] means IDs of this type are case-insensitive, i.e., values that only differ in casing
 * refer to the same entity.
 */
enum class ExternalIdType(val displayName: String, val canonicalCase: CanonicalCase = CanonicalCase.AS_IS) {
    /**
     * [Digital Object Identifier](https://www.doi.org/). DOIs are case-insensitive, see the
     * [DOI Handbook](https://www.doi.org/doi-handbook/html/index.html), chapter 3.3.4.
     */
    DOI("DOI", canonicalCase = CanonicalCase.LOWER),

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

    /**
     * [OpenAlex](https://openalex.org/). OpenAlex IDs are case-insensitive, e.g. `w2741809807` resolves to the same
     * work as `W2741809807`, and are uppercased since the uppercase form is the one OpenAlex returns.
     */
    OPEN_ALEX("OpenAlex", canonicalCase = CanonicalCase.UPPER),

    /**
     * [IEEE Xplore](https://ieeexplore.ieee.org/).
     */
    IEEE_XPLORE("IEEE Xplore"),
}

/**
 * The casing that values of an [ExternalIdType] are converted to during normalization.
 */
enum class CanonicalCase {
    /** The value is case-sensitive and its casing is kept as is. */
    AS_IS,

    /** The value is case-insensitive and lowercased. */
    LOWER,

    /** The value is case-insensitive and uppercased. */
    UPPER,
}
