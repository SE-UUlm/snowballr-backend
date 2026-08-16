package se.uulm.snowballr.backend.matching

import se.uulm.snowballr.backend.model.dto.paper.Author

object Tokenization {
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
        val cleaned = fullName.lowercase().replace(Regex("[^a-z0-9 ]"), "")
        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val result = mutableSetOf<String>()
        for (token in tokens) {
            result.add(token)
            if (token.length > 1) result.add(token[0].toString())
        }

        return result
    }
}
