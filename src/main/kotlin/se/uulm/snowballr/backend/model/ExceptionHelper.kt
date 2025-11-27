package se.uulm.snowballr.backend.model

/**
 * Constructs a formatted string representation of the provided entity IDs, incorporating
 * an optional identifier type for contextualizing the IDs.
 *
 * @param entityIds A list of entity IDs to be displayed in the resulting string.
 * @param identifierType The type of identifier associated with the entity IDs. Defaults to [IdentifierType.ID].
 * @return A string containing the formatted representation of the entity IDs prefixed with the specified identifier type.
 */
fun displayEntityIds(entityIds: List<Any>, identifierType: IdentifierType = IdentifierType.ID): String {
    require(entityIds.isNotEmpty()) { "Cannot display empty list of entity IDs." }
    if (entityIds.size == 1) {
        return "with ${identifierType.displayName} '${entityIds[0]}'"
    }
    val idString = entityIds.dropLast(1).joinToString(separator = ", ") { "'$it'" }
    return "with ${identifierType.displayName} $idString and '${entityIds.last()}'"
}
