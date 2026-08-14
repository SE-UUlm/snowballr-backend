package se.uulm.snowballr.backend.fetcher.normalization

import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.isNotBlank
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import java.text.Normalizer

/**
 * Normalizes [FetcherPaper]s so that semantically equivalent data coming from different fetchers is represented
 * consistently, which improves deduplication and matching.
 *
 * Free-text fields are Unicode-normalized to NFC (Normalization Form C, canonical composition), have typographic
 * punctuation variants (quotes, dashes) folded to their plain equivalents, and have their whitespace collapsed and
 * trimmed. NFC matters because Unicode allows the same visible character to be encoded in more than one way: for
 * example "é" can be a single precomposed code point (U+00E9), or the two code points "e" + a combining acute
 * accent (U+0065 U+0301). Both render identically but are different strings byte-for-byte, so two fetchers could
 * report the "same" title or author name that fails an equality or similarity check purely due to encoding.
 * NFC composes such sequences into their single precomposed form wherever one exists, without otherwise altering
 * the text (unlike the compatibility forms NFKC/NFKD, which also fold things like ligatures or superscripts and
 * would be lossy for display). Blank authors and external IDs are dropped. External ID values are only trimmed,
 * since they are identifiers rather than prose and must not be altered.
 */
object PaperNormalizer {
    private val WHITESPACE_REGEX = Regex("[\\s\\u00A0]+")

    private val PUNCTUATION_REPLACEMENTS = mapOf(
        '‘' to '\'', // left single quotation mark
        '’' to '\'', // right single quotation mark
        '“' to '"', // left double quotation mark
        '”' to '"', // right double quotation mark
        '–' to '-', // en dash
        '—' to '-', // em dash
        '−' to '-', // minus sign
    )

    fun normalize(paper: FetcherPaper): FetcherPaper = paper.copy(
        title = normalizeText(paper.title),
        abstract = normalizeText(paper.abstract),
        publisher = normalizeText(paper.publisher),
        publicationType = normalizeText(paper.publicationType),
        publicationName = normalizeText(paper.publicationName),
        authors = paper.authors.map { normalizeAuthor(it) }.filter { it.isNotBlank() },
        externalIds = paper.externalIds
            .map { it.copy(value = it.value.trim()) }
            .filter { it.value.isNotBlank() },
    )

    /**
     * Unicode-normalizes [value] to NFC (see class docs for why), folds typographic punctuation variants to their
     * plain equivalents, and collapses and trims whitespace.
     */
    fun normalizeText(value: String): String {
        val composed = Normalizer.normalize(value, Normalizer.Form.NFC)
        val withPlainPunctuation = composed.map { PUNCTUATION_REPLACEMENTS[it] ?: it }.joinToString("")
        return withPlainPunctuation.replace(WHITESPACE_REGEX, " ").trim()
    }

    private fun normalizeAuthor(author: Author) = author.copy(
        firstName = normalizeText(author.firstName),
        lastName = normalizeText(author.lastName),
    )
}
