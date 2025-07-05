package se.uulm.snowballr.backend.model

/**
 * Represents the type of access being attempted on an entity.
 */
enum class AccessType(val description: String) {
    /** Attempt to read or access an entity */
    READ("read"),

    /** Attempt to create a new entity */
    CREATE("create"),

    /** Attempt to update an existing entity */
    UPDATE("update"),

    /** Attempt to delete an entity */
    DELETE("delete"),
    ;

    override fun toString(): String = description
}
