package se.uulm.snowballr.backend.model

/**
 * Enum that represents different identifiers for users.
 */
enum class UserIdentifierType {
    ID,
    EMAIL,
    ;

    fun toIdentifierType() = when (this) {
        ID -> IdentifierType.ID
        EMAIL -> IdentifierType.EMAIL
    }
}
