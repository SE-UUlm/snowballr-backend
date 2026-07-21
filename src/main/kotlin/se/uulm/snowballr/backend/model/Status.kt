package se.uulm.snowballr.backend.model

@Suppress("MagicNumber")
enum class Status(val code: Int) {
    // --- Client Error --- //

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),

    // --- Server Error -- //

    INTERNAL(500),
}
