package se.uulm.snowballr.backend.model

/**
 * Enum that represents the different entities in our server and their names for logging.
 *
 * @property singular The singular form of the entity's name.
 * @property plural The plural form of the entity's name.
 */
enum class EntityType(val singular: String, val plural: String) {
    USER("user", "users"),
    PROJECT("project", "projects"),
    CRITERION("criterion", "criteria"),
    PROJECT_MEMBER("project member", "project members"),
    PAPER("paper", "papers"),
    AUTHOR("author", "authors"),
    VERIFICATION_TOKEN("verification token", "verification tokens"),
    ;

    /**
     * Converts the [singular] value to have the first char in upper case.
     */
    fun singularUpper(): String = this.singular.replaceFirstChar { it.uppercase() }
}
