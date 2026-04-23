package se.uulm.snowballr.backend.model.fetcher

/**
 * Captures the full output of a finished fetcher process.
 *
 * @property stdout Standard output emitted by the process.
 * @property stderr Standard error emitted by the process.
 * @property returnCode Exit code returned by the process.
 */
data class ProcessResult(
    val stdout: String,
    val stderr: String,
    val returnCode: Int,
)
