package se.uulm.snowballr.backend.matching

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class LevenshteinTest {
    companion object {
        @JvmStatic
        fun distanceExamples(): List<Arguments> = listOf(
            Arguments.of("kitten", "sitting", 3),
            Arguments.of("flaw", "lawn", 2),
            Arguments.of("intention", "execution", 5),
            Arguments.of("abc", "abd", 1),
            Arguments.of("abc", "abcd", 1),
            Arguments.of("abcd", "abc", 1),
            Arguments.of("abc", "ab", 1),
            Arguments.of("book", "back", 2),
            Arguments.of("a", "b", 1),
        )

        @JvmStatic
        fun identicalStringExamples(): List<Arguments> = listOf(
            Arguments.of(""),
            Arguments.of("a"),
            Arguments.of("abc"),
            Arguments.of("Hello, World!"),
        )

        @JvmStatic
        fun caseSensitivityExamples(): List<Arguments> = listOf(
            Arguments.of("abc", "ABC", 3),
            Arguments.of("Abc", "abc", 1),
        )

        @JvmStatic
        fun normalizedDistanceExamples(): List<Arguments> = listOf(
            Arguments.of("abc", "abc", 1.0),
            Arguments.of("abc", "abd", 1.0 - 1.0 / 3.0),
            Arguments.of("a", "b", 0.0),
            Arguments.of("abc", "xyz", 0.0),
            Arguments.of("abc", "", 0.0),
            Arguments.of("", "abc", 0.0),
            Arguments.of("kitten", "sitting", 1.0 - 3.0 / 7.0),
        )
    }

    @Nested
    inner class GetDistance {
        @Test
        fun `When both strings are empty, then distance is zero`() {
            val score = Levenshtein.getDistance("", "")

            assertEquals(0, score)
        }

        @Test
        fun `When the first string is empty, then distance equals the second string length`() {
            val score = Levenshtein.getDistance("", "abc")

            assertEquals(3, score)
        }

        @Test
        fun `When the second string is empty, then distance equals the first string length`() {
            val score = Levenshtein.getDistance("abc", "")

            assertEquals(3, score)
        }

        @ParameterizedTest(name = "distance({0}, {0}) == 0")
        @MethodSource("se.uulm.snowballr.backend.matching.LevenshteinTest#identicalStringExamples")
        fun `When both strings are identical, then distance is zero`(value: String) {
            val score = Levenshtein.getDistance(value, value)

            assertEquals(0, score)
        }

        @ParameterizedTest(name = "distance({0}, {1}) == {2}")
        @MethodSource("se.uulm.snowballr.backend.matching.LevenshteinTest#distanceExamples")
        fun `When strings differ, then the expected edit distance is returned`(a: String, b: String, expected: Int) {
            val score = Levenshtein.getDistance(a, b)

            assertEquals(expected, score)
        }

        @ParameterizedTest(name = "distance({0}, {1}) == {2}")
        @MethodSource("se.uulm.snowballr.backend.matching.LevenshteinTest#caseSensitivityExamples")
        fun `When strings only differ in casing, then casing is treated as a difference`(
            a: String,
            b: String,
            expected: Int,
        ) {
            val score = Levenshtein.getDistance(a, b)

            assertEquals(expected, score)
        }

        @ParameterizedTest(name = "distance({0}, {1}) == distance({1}, {0})")
        @MethodSource("se.uulm.snowballr.backend.matching.LevenshteinTest#distanceExamples")
        fun `When arguments are swapped, then distance is symmetric`(a: String, b: String) {
            val score = Levenshtein.getDistance(a, b)
            val swappedScore = Levenshtein.getDistance(b, a)

            assertEquals(score, swappedScore)
        }
    }

    @Nested
    inner class GetNormalizedDistance {
        @Test
        fun `When both strings are empty, then normalized distance is one`() {
            val score = Levenshtein.getNormalizedDistance("", "")

            assertEquals(1.0, score)
        }

        @ParameterizedTest(name = "normalizedDistance({0}, {1}) == {2}")
        @MethodSource("se.uulm.snowballr.backend.matching.LevenshteinTest#normalizedDistanceExamples")
        fun `When strings are compared, then the expected normalized distance is returned`(
            a: String,
            b: String,
            expected: Double,
        ) {
            val score = Levenshtein.getNormalizedDistance(a, b)

            assertEquals(expected, score, 1e-9)
        }
    }
}
