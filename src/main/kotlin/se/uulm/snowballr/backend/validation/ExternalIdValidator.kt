package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.InvalidExternalIdFormat
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import snowballr.PaperOuterClass.Paper.ExternalId

object ExternalIdValidator {
    const val EXTERNAL_ID_MAX_LENGTH = 100

    /**
     * Matches a [DOI](https://www.doi.org/) of the form `10.<4-9 digit registrant code>/<suffix>`, e.g.
     * `10.1000/xyz123`.
     */
    private val DOI_REGEX = Regex("""^10\.\d{4,9}/\S+$""")

    /**
     * Matches an [ArXiv](https://arxiv.org/) identifier in either the current `YYMM.NNNNN` scheme introduced in
     * 2007 (e.g. `2101.00001`, optionally with a `vN` version suffix) or the legacy `archive/YYMMNNN` scheme used
     * before that (e.g. `hep-th/9901001`).
     */
    private val ARXIV_REGEX = Regex("""^\d{4}\.\d{4,5}(v\d+)?$|^[a-z-]+(\.[A-Z]{2})?/\d{7}(v\d+)?$""")

    /**
     * Matches a numeric ID as used by both
     * [Microsoft Academic Graph](https://www.microsoft.com/en-us/research/project/microsoft-academic-graph/) (MAG)
     * and [PubMed](https://pubmed.ncbi.nlm.nih.gov/)/[Medline](https://www.nlm.nih.gov/medline/medline_home.html)
     * (PMID), e.g. `12345678`.
     */
    private val DIGITS_ONLY_REGEX = Regex("""^\d+$""")

    /**
     * Matches a [PubMed Central](https://pmc.ncbi.nlm.nih.gov/) ID, i.e. the `PMC` prefix followed by digits, e.g.
     * `PMC1234567`.
     */
    private val PUB_MED_CENTRAL_REGEX = Regex("""^PMC\d+$""")

    /**
     * Matches a [Semantic Scholar](https://www.semanticscholar.org/) paper ID, a 40 character lowercase hex string.
     */
    private val SEMANTIC_SCHOLAR_REGEX = Regex("""^[a-f0-9]{40}$""")

    fun validateExternalId(externalId: ExternalId): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureValidEnumValue<ExternalIdType>(externalId.type, "External ID Type") },
            { ensureTextFieldValidity("value", externalId.value, EXTERNAL_ID_MAX_LENGTH) },
            { ensureExternalIdFormatValidity(externalId.type, externalId.value) },
        ) { _, _, _ -> }
    }

    /**
     * The expected value format for the given [ExternalIdType], or `null` if the type does not have a
     * standardized-enough format to validate meaningfully ([ExternalIdType.ACL]'s Anthology ID scheme and
     * [ExternalIdType.DBLP]'s key scheme have both changed shape too many times over the years), in which case
     * only the generic blank/max-length checks apply.
     */
    private fun formatRegexFor(type: ExternalIdType): Regex? = when (type) {
        ExternalIdType.DOI -> DOI_REGEX
        ExternalIdType.ARXIV -> ARXIV_REGEX
        ExternalIdType.MAG -> DIGITS_ONLY_REGEX
        ExternalIdType.PUB_MED -> DIGITS_ONLY_REGEX
        ExternalIdType.MEDLINE -> DIGITS_ONLY_REGEX
        ExternalIdType.PUB_MED_CENTRAL -> PUB_MED_CENTRAL_REGEX
        ExternalIdType.SEMANTIC_SCHOLAR -> SEMANTIC_SCHOLAR_REGEX
        ExternalIdType.ACL -> null
        ExternalIdType.DBLP -> null
    }

    /**
     * Ensures that the given external ID [value] matches the format expected for its [type], if such a format is
     * defined by [formatRegexFor].
     *
     * Unknown or unspecified types are ignored here since they are already covered by the enum validity check.
     *
     * @param type The raw external ID type string.
     * @param value The external ID value to check for format validity.
     */
    private fun Raise<ValidationIssue>.ensureExternalIdFormatValidity(type: String, value: String) {
        val regex = runCatching { enumValueOf<ExternalIdType>(type) }.getOrNull()?.let { formatRegexFor(it) }
            ?: return
        ensure(regex.matches(value)) { InvalidExternalIdFormat(type, value) }
    }
}
