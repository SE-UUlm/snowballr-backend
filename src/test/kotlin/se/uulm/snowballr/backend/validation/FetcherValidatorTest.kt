package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.FetcherValidator.NAME_MAX_LENGTH
import snowballr.Fetcher

class FetcherValidatorTest {
    @Nested
    inner class GetAvailableFetcherOptions {
        private val validGetAvailableFetcherOptionsRequestBuilder: Fetcher.GetAvailableFetcherOptionsRequest.Builder =
            Fetcher.GetAvailableFetcherOptionsRequest
                .newBuilder()
                .setFetcherName("Valid Fetcher Name")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validGetAvailableFetcherOptionsRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank name is validated, then the 'BlankField' issue is returned`() {
            val request =
                validGetAvailableFetcherOptionsRequestBuilder
                    .setFetcherName("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When a too long name is validated, then the 'TooLongField' issue is returned`() {
            val request =
                validGetAvailableFetcherOptionsRequestBuilder
                    .setFetcherName("a".repeat(NAME_MAX_LENGTH + 1))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }
    }
}
