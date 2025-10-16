package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.InvalidOrcid
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.PaperOuterClass.Author

object AuthorValidator {
    const val FIRST_NAME_MAX_LENGTH = 100
    const val LAST_NAME_MAX_LENGTH = 100
    private val orcidRegex = Regex("""^\d{4}-\d{4}-\d{4}-\d{3}[0-9X]$""")

    fun validateAuthor(author: Author): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureTextFieldValidity("first_name", author.firstName, FIRST_NAME_MAX_LENGTH) },
            { ensureTextFieldValidity("last_name", author.lastName, LAST_NAME_MAX_LENGTH) },
            { ensureOrcidValidity(author.orcid) },
        ) { _, _, _ -> }
    }

    /**
     * Ensures that the provided ORCID is valid.
     *
     * See
     * [orcid.org](https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier)
     * for more information.
     */
    private fun Raise<ValidationIssue>.ensureOrcidValidity(orcid: String) {
        if (orcid.isEmpty()) return
        ensureOrcidFormat(orcid)
        ensureOrcidCheckDigitValidity(orcid)
    }

    /**
     * Ensures that the ORCID format is valid.
     *
     * The ORCID is defined as follows:
     * - four groups of digits separated by hyphens.
     * - the last digit can also be an 'X' (check digit).
     */
    private fun Raise<ValidationIssue>.ensureOrcidFormat(orcid: String) {
        ensure(orcidRegex.matches(orcid)) {
            InvalidOrcid(orcid, InvalidOrcid.Reason.INVALID_FORMAT)
        }
    }

    /**
     * Ensures that the ORCID MOD11 check digit is valid.
     */
    private fun Raise<ValidationIssue>.ensureOrcidCheckDigitValidity(orcid: String) {
        val baseDigits = orcid.replace("-", "").dropLast(1)
        val expectedCheckDigit = generateCheckDigit(baseDigits)
        val isMatch = orcid.last() == expectedCheckDigit
        ensure(isMatch) { InvalidOrcid(orcid, InvalidOrcid.Reason.INVALID_CHECK_DIGIT) }
    }

    /**
     * Algorithm for calculating the ORCID check digit.
     *
     * See
     * [orcid.org](https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier#h_01HP0RGP0YWYZC9CGFX2P09HGM)
     * for more information.
     */
    @Suppress("MagicNumber")
    fun generateCheckDigit(baseDigits: String): Char {
        var total = 0
        for (i in 0..<baseDigits.length) {
            val digit = Character.getNumericValue(baseDigits[i])
            total = (total + digit) * 2
        }
        val remainder = total % 11
        val result = (12 - remainder) % 11
        return if (result == 10) 'X' else result.toString()[0]
    }
}
