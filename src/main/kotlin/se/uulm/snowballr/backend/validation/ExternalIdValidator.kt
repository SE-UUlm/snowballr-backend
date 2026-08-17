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
     * Matches a [DOI](https://www.doi.org/) of the form `10.<registrant code of 4 or more digits>/<suffix>`, e.g.
     * `10.1000/xyz123` or `10.48550/arXiv.2202.01037`.
     *
     * [Scheme](https://www.doi.org/the-identifier/what-is-a-doi/).
     */
    private val DOI_REGEX = Regex("""^10\.\d{4,}/\S+$""")

    /**
     * Matches the current [ArXiv](https://arxiv.org/) `YYMM.NNNNN` scheme introduced in 2007, e.g. `2101.00001`,
     * optionally with a `vN` version suffix, e.g. `2101.00001v2`.
     */
    private val ARXIV_NEW_STYLE_REGEX = Regex("""^\d{4}\.\d{4,5}(v\d+)?$""")

    /**
     * Matches the legacy [ArXiv](https://arxiv.org/) `archive.subject-class/YYMMNNN` scheme used before the
     * `YYMM.NNNNN` scheme was introduced in 2007, e.g. `hep-th/9901001`.
     *
     * An ArXiv identifier matches either this or the [ARXIV_NEW_STYLE_REGEX] scheme.
     * [Scheme](https://info.arxiv.org/help/arxiv_identifier.html).
     */
    private val ARXIV_OLD_STYLE_REGEX = Regex("""^[a-z-]+(\.[A-Z]{2})?/\d{7}(v\d+)?$""")

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

    /**
     * Matches the current [ACL Anthology](https://aclanthology.org/) ID scheme introduced in 2020,
     * `YYYY.<venue>-<volume>.<paper>`, e.g. `2021.acl-long.1`.
     */
    private val ACL_NEW_STYLE_REGEX = Regex("""^\d{4}\.[a-z0-9]+-[a-z0-9]+\.\d+$""")

    /**
     * Matches the legacy [ACL Anthology](https://aclanthology.org/) ID scheme used before 2020,
     * `<collection letter><2 digit year>-<paper number>`, e.g. `P19-1007`.
     *
     * An ACL Anthology identifier matches either this or the [ACL_NEW_STYLE_REGEX] scheme.
     * [Scheme](https://aclanthology.org/info/ids/).
     */
    private val ACL_OLD_STYLE_REGEX = Regex("""^[A-Z]\d{2}-\d{3,4}$""")

    /**
     * Matches a [dblp](https://dblp.org/) record key of the form `<prefix>/<conference-or-journal>/<id-suffix>`,
     * e.g. `journals/tods/Bernstein83` or `journals/corr/abs-2103-05387`.
     *
     * [Scheme](https://dblp.org/xml/docu/dblpxml.pdf).
     */
    private val DBLP_REGEX = Regex("""^[a-z]+(/[A-Za-z0-9_-]+)+$""")

    fun validateExternalId(externalId: ExternalId): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureValidEnumValue<ExternalIdType>(externalId.type, "External ID Type") },
            {
                ensureTextFieldValidity("value", externalId.value, EXTERNAL_ID_MAX_LENGTH)
                ensureExternalIdFormatValidity(externalId.type, externalId.value.trim())
            },
        ) { _, _ -> }
    }

    /**
     * Whether the given [value] matches the format expected for the given [type].
     */
    private fun matchesExpectedFormat(type: ExternalIdType, value: String): Boolean = when (type) {
        ExternalIdType.DOI -> DOI_REGEX.matches(value)
        ExternalIdType.ARXIV -> ARXIV_NEW_STYLE_REGEX.matches(value) || ARXIV_OLD_STYLE_REGEX.matches(value)
        ExternalIdType.MAG, ExternalIdType.PUB_MED, ExternalIdType.MEDLINE -> DIGITS_ONLY_REGEX.matches(value)
        ExternalIdType.PUB_MED_CENTRAL -> PUB_MED_CENTRAL_REGEX.matches(value)
        ExternalIdType.SEMANTIC_SCHOLAR -> SEMANTIC_SCHOLAR_REGEX.matches(value)
        ExternalIdType.ACL -> ACL_NEW_STYLE_REGEX.matches(value) || ACL_OLD_STYLE_REGEX.matches(value)
        ExternalIdType.DBLP -> DBLP_REGEX.matches(value)
    }

    /**
     * Ensures that the given external ID [value] matches the format expected for its [type], as defined by
     * [matchesExpectedFormat].
     *
     * Unknown or unspecified types are ignored here since they are already covered by the enum validity check.
     *
     * @param type The raw external ID type string.
     * @param value The external ID value to check for format validity.
     */
    private fun Raise<ValidationIssue>.ensureExternalIdFormatValidity(type: String, value: String) {
        val parsedType = runCatching { enumValueOf<ExternalIdType>(type) }.getOrNull() ?: return
        ensure(matchesExpectedFormat(parsedType, value)) { InvalidExternalIdFormat(type, value) }
    }
}
