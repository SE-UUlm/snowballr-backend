package se.uulm.snowballr.backend.model

/**
 * Constructs a formatted string representation of the provided entity IDs, incorporating
 * an optional identifier type for contextualizing the IDs.
 *
 * @param entityIds A list of entity IDs to be displayed in the resulting string.
 * @param identifierType The type of identifier associated with the entity IDs. Defaults to [IdentifierType.ID].
 * @return A string containing the formatted representation of the entity IDs prefixed with the specified identifier type.
 */
fun displayEntityIds(entityIds: List<String>, identifierType: IdentifierType = IdentifierType.ID): String {
    var idString = entityIds.dropLast(1).joinToString(
        separator = ", ",
    ) { "'$it'" }

    if (entityIds.isNotEmpty()) {
        idString += " and '${entityIds.last()}'"
    }
    return "with ${identifierType.displayName} $idString"
}
