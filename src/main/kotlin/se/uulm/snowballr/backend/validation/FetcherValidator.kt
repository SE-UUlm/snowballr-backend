package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.Fetcher

object FetcherValidator {
    const val NAME_MAX_LENGTH = 100

    fun validateGetAvailableFetcherOptionsRequest(
        request: Fetcher.GetAvailableFetcherOptionsRequest,
    ): EitherNel<ValidationIssue, Unit> = either {
        ensureTextFieldValidity("fetcher_name", request.fetcherName, NAME_MAX_LENGTH)
    }.toEitherNel()
}
