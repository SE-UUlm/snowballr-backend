package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents an exception that occurs when a fetcher path traverses outside the configured fetchers directory.
 */
class UnauthorizedFetcherPathException(fetcher: String) : SnowballRException(
    Status.INTERNAL,
    "Fetcher \"$fetcher\" is outside the configured fetchers directory.",
)
