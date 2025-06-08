package se.uulm.snowballr.backend.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * This class contains tests that ensure that the ordinal values of the enums have the same value as they were expected.
 * This prevents the values from changing over time, which might cause problems with database migration, if the enum is
 * stored in the database.
 *
 * If a new enum value is added, then it should be put at the end so that it has the next greater ordinal value.
 */
class EnumOrdinalTest {
    @Test
    fun `When the FetcherApi values are read, then they have the expected ordinal values`() {
        for (value in FetcherApi.entries) {
            val expectedOrdinal =
                when (value) {
                    FetcherApi.GOOGLE_SCHOLAR -> 0
                    FetcherApi.SEMANTIC_SCHOLAR -> 1
                    FetcherApi.IEEE_XPLORE -> 2
                }
            assertThat(value.ordinal).isEqualTo(expectedOrdinal)
        }
    }
}
