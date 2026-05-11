package se.uulm.snowballr.backend.table.columntypes

import org.jetbrains.exposed.v1.core.TextColumnType

/**
 * [TextColumnType], but the value is obfuscated for logging.
 */
class ObfuscatedTextColumnType(
    collate: String? = null,
    eagerLoading: Boolean = false,
) : TextColumnType(collate, eagerLoading) {
    override fun valueToString(value: String?) = "[OBFUSCATED]"
}
