package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.sql.TextColumnType

/**
 * [TextColumnType], but the value is obfuscated for logging.
 */
class ObfuscatedTextColumnType(
    collate: String? = null,
    eagerLoading: Boolean = false,
) : TextColumnType(collate, eagerLoading) {
    override fun valueToString(value: String?) = "[OBFUSCATED]"
}
