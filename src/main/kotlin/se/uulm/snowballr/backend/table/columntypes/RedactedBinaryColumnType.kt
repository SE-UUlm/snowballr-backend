package se.uulm.snowballr.backend.table.columntypes

import org.jetbrains.exposed.v1.core.BasicBinaryColumnType

/**
 * [BasicBinaryColumnType], but the value is redacted for logging.
 */
class RedactedBinaryColumnType : BasicBinaryColumnType() {
    override fun valueToString(value: ByteArray?) = "[REDACTED BINARY]"
}
