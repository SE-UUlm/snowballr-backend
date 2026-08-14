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

    private val DIGITS_ONLY_REGEX = Regex("""^\d+$""")

    fun validateExternalId(externalId: ExternalId): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureValidEnumValue<ExternalIdType>(externalId.type, "External ID Type") },
            { ensureTextFieldValidity("value", externalId.value, EXTERNAL_ID_MAX_LENGTH) },
            { ensureExternalIdFormatValidity(externalId.type, externalId.value) },
        ) { _, _, _ -> }
    }

    /**
     * The expected value format for the given [ExternalIdType], or `null` if the type does not have a
     * standardized-enough format to validate meaningfully (e.g. [ExternalIdType.ACL], [ExternalIdType.DBLP]),
     * in which case only the generic blank/max-length checks apply.
     *
     * This is a `when` over all [ExternalIdType] entries without an `else` branch so that adding a new type
     * without deciding on its format fails compilation.
     */
    private fun formatRegexFor(type: ExternalIdType): Regex? = when (type) {
        ExternalIdType.DOI -> Regex("""^10\.\d{4,9}/\S+$""")
        ExternalIdType.ARXIV -> Regex("""^\d{4}\.\d{4,5}(v\d+)?$|^[a-z-]+(\.[A-Z]{2})?/\d{7}(v\d+)?$""")
        ExternalIdType.MAG -> DIGITS_ONLY_REGEX
        ExternalIdType.PUB_MED -> DIGITS_ONLY_REGEX
        ExternalIdType.MEDLINE -> DIGITS_ONLY_REGEX
        ExternalIdType.PUB_MED_CENTRAL -> Regex("""^PMC\d+$""")
        ExternalIdType.SEMANTIC_SCHOLAR -> Regex("""^[a-f0-9]{40}$""")
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
