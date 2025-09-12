package se.uulm.snowballr.backend.model

/**
 * Enum that represents different identifiers for entities.
 *
 * @property displayName The displayable name of the identifier, which can be used in logs or user messages.
 */
enum class IdentifierType(val displayName: String) {
    ID("ID"),
    EMAIL("email"),
    LOCAL_ID("local ID"),
}
