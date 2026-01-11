package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.sql.BasicBinaryColumnType

/**
 * [BasicBinaryColumnType], but the value is redacted for logging.
 */
class RedactedBinaryColumnType : BasicBinaryColumnType() {
    override fun valueToString(value: ByteArray?) = "[REDACTED BINARY]"
}
