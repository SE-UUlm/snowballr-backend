package se.uulm.snowballr.backend.model.dto.paper

enum class ExternalIdType(val displayName: String) {
    /**
     * [Digital Object Identifier](https://www.doi.org/).
     */
    DOI("DOI"),

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
