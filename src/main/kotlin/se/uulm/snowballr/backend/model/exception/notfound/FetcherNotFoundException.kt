package se.uulm.snowballr.backend.model.exception.notfound

import se.uulm.snowballr.backend.model.exception.NotFoundException

/**
 * Represents an exception that occurs when a fetcher could not be found.
 */
class FetcherNotFoundException(name: String) : NotFoundException("Fetcher \"${name}\" not found.")
