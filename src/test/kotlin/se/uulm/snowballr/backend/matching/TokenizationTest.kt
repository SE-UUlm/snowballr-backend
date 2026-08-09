package se.uulm.snowballr.backend.matching

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.dto.paper.Author

class TokenizationTest {
    companion object {
        @JvmStatic
        fun authorTokensExamples(): List<Arguments> = listOf(
            Arguments.of(Author("John", "Doe"), setOf("john", "j", "doe", "d")),
            Arguments.of(Author("Jane", "Doe"), setOf("jane", "j", "doe", "d")),
            Arguments.of(Author("JOHN", "DOE"), setOf("john", "j", "doe", "d")),
            Arguments.of(Author("Jean-Paul", "Sartre"), setOf("jeanpaul", "j", "sartre", "s")),
            Arguments.of(Author("Conan", "O'Brien"), setOf("conan", "c", "obrien", "o")),
            Arguments.of(Author("Louis", "XIV2"), setOf("louis", "l", "xiv2", "x")),
            Arguments.of(Author("John", "  Doe"), setOf("john", "j", "doe", "d")),
            Arguments.of(Author("X", "Yu"), setOf("x", "yu", "y")),
        )

        @JvmStatic
        fun blankAuthorExamples(): List<Arguments> = listOf(
            Arguments.of(Author("", "")),
            Arguments.of(Author(" ", " ")),
            Arguments.of(Author("", " ")),
        )
    }

    @Nested
    inner class AuthorTokens {
        @ParameterizedTest(name = "authorTokens({0}) == {1}")
        @MethodSource("se.uulm.snowballr.backend.matching.TokenizationTest#authorTokensExamples")
        fun `When tokens are created for an author, then the expected tokens are returned`(
            author: Author,
            expected: Set<String>,
        ) {
            val tokens = Tokenization.authorTokens(author)

            assertEquals(expected, tokens)
        }

        @ParameterizedTest(name = "authorTokens({0}) is empty")
        @MethodSource("se.uulm.snowballr.backend.matching.TokenizationTest#blankAuthorExamples")
        fun `When both name fields are blank, then no tokens are returned`(author: Author) {
            val tokens = Tokenization.authorTokens(author)

            assertEquals(emptySet<String>(), tokens)
        }
    }

    @Nested
    inner class AuthorSetTokens {
        @Test
        fun `When the author list is empty, then no tokens are returned`() {
            val tokens = Tokenization.authorSetTokens(emptyList())

            assertEquals(emptySet<String>(), tokens)
        }

        @Test
        fun `When a single author is passed, then the result equals their individual tokens`() {
            val author = Author("Jane", "Doe")

            val tokens = Tokenization.authorSetTokens(listOf(author))

            assertEquals(Tokenization.authorTokens(author), tokens)
        }

        @Test
        fun `When multiple authors share overlapping tokens, then the tokens are merged without duplicates`() {
            val authors = listOf(Author("John", "Doe"), Author("Jane", "Doe"))

            val tokens = Tokenization.authorSetTokens(authors)

            assertEquals(setOf("john", "j", "doe", "d", "jane"), tokens)
        }

        @Test
        fun `When multiple authors have distinct names, then all of their tokens are included`() {
            val authors = listOf(Author("Alice", "Smith"), Author("Bob", "Jones"))

            val tokens = Tokenization.authorSetTokens(authors)

            assertEquals(setOf("alice", "a", "smith", "s", "bob", "b", "jones", "j"), tokens)
        }
    }
}
