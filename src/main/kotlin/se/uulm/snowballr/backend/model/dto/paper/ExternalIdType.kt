package se.uulm.snowballr.backend.model.dto.paper

enum class ExternalIdType {
    /**
     * [Digital Object Identifier](https://www.doi.org/).
     */
    DOI,

    /**
     * [arXiv](https://arxiv.org/).
     */
    ARXIV,

    /**
     * [Microsoft Academic Graph](https://www.microsoft.com/en-us/research/project/microsoft-academic-graph/).
     */
    MAG,

    /**
     * [Association of Computational Linguistics](https://www.aclweb.org/anthology/).
     */
    ACL,

    /**
     * [PubMed](https://pubmed.ncbi.nlm.nih.gov/).
     */
    PUB_MED,

    /**
     * [Medline](https://www.nlm.nih.gov/medline/medline_home.html).
     */
    MEDLINE,

    /**
     * [PubMed Central](https://pmc.ncbi.nlm.nih.gov/).
     */
    PUB_MED_CENTRAL,

    /**
     * [dblp](https://dblp.org/).
     */
    DBLP,

    /**
     * [Semantic Scholar](https://www.semanticscholar.org/).
     */
    SEMANTIC_SCHOLAR,

    /**
     * Arbitrary URL.
     */
    URL,
}
