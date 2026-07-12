package se.uulm.snowballr.backend.matching

import se.uulm.snowballr.backend.matching.Levenshtein.getDistance

object Levenshtein {
    /**
     * Returns the Levenshtein distance between the two passed strings.
     *
     * The Levenshtein distance is a string metric for measuring the difference between two sequences. It's the minimum
     * number of single-character edits (insertions, deletions or substitutions) required to change one sequence into
     * the other.
     */
    fun getDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val curr = IntArray(b.length + 1)
            curr[0] = i
            for (j in 1..b.length) {
                curr[j] = if (a[i - 1] == b[j - 1]) {
                    prev[j - 1]
                } else {
                    minOf(prev[j - 1], prev[j], curr[j - 1]) + 1
                }
            }
            prev = curr
        }

        return prev[b.length]
    }

    /**
     * Returns the normalized Levenshtein distance between the two passed strings. Higher means more similarity.
     *
     * The same as [getDistance], but the distance is normalized according to the maximum length of both sequences.
     * If both sequences are empty, the normalized distance is 1. Otherwise, the result is calculated by the following
     * formula: `1 - (levensthein(a, b) / max(a.length, b.length))`
     */
    fun getNormalizedDistance(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        return 1.0 - getDistance(a, b).toDouble() / maxOf(a.length, b.length)
    }
}
