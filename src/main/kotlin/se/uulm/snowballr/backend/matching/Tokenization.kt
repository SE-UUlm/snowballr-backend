package se.uulm.snowballr.backend.matching

import se.uulm.snowballr.backend.model.dto.paper.Author
import java.text.Normalizer

/**
 * Tokenizes author names for use in similarity comparisons (see [PaperMatcher]).
 *
 * Diacritics are folded (e.g. "Jürgen" -> "jurgen") rather than dropped, and letters from non-Latin scripts
 * (e.g. Cyrillic, CJK) are kept rather than stripped, so that names in any script and encoding still produce
 * meaningful, comparable tokens.
 *
 * To fold diacritics, the name is Unicode-normalized to NFD (Normalization Form D, canonical decomposition), which
 * splits a precomposed accented letter into its base letter plus one or more combining marks - for example "ü"
 * becomes "u" + a combining diaeresis (U+0308). Those combining marks are then stripped, leaving only the base
 * letter. This is the opposite of the NFC used by [se.uulm.snowballr.backend.fetcher.normalization.PaperNormalizer] to
 * canonicalize stored values: NFC composes marks back into a single character for consistent storage, NFD decomposes
 * them so they can be isolated and removed here.
 */
object Tokenization {
    private val COMBINING_MARKS_REGEX = Regex("""\p{Mn}+""")
    private val NON_LETTER_OR_DIGIT_REGEX = Regex("""[^\p{L}\p{N}\s]""")

    /**
     * Creates a set of tokens for all passed [authors].
     *
     * Example:
     * - Authors: "John Doe" and "Jane Doe"
     * - Tokens: "john", "j", "doe", "d", "jane"
     */
    fun authorSetTokens(authors: List<Author>): Set<String> = authors.flatMap { authorTokens(it) }.toSet()

    /**
     * Creates a set of tokens for the passed [author].
     *
     * Tokens are a set of strings used to compare authors.
     * They consist of the words of the full name and the first character of each word.
     * Including the first character, enables comparing abbreviated words of the full author name.
     *
     * Tokens for example author "John Doe": "john", "j", "doe", "d"
     */
    fun authorTokens(author: Author): Set<String> {
        val fullName = "${author.firstName} ${author.lastName}".trim()
        val foldedAccents = Normalizer.normalize(fullName, Normalizer.Form.NFD).replace(COMBINING_MARKS_REGEX, "")
        val cleaned = foldedAccents.lowercase().replace(NON_LETTER_OR_DIGIT_REGEX, "")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val result = mutableSetOf<String>()
        for (token in tokens) {
            result.add(token)
            if (token.length > 1) result.add(token[0].toString())
        }

        return result
    }
}
