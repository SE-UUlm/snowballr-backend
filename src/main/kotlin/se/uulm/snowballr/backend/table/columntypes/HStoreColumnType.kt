package se.uulm.snowballr.backend.table.columntypes

import org.jetbrains.exposed.v1.core.ColumnType
import org.postgresql.util.HStoreConverter
import org.postgresql.util.PGobject

/**
 * Uses PostgreSQL's HSTORE extension to store key-value pairs in a single column.
 */
class HStoreColumnType : ColumnType<Map<String, String>>() {
    override fun sqlType(): String = "HSTORE"

    @Suppress("UNCHECKED_CAST", "NullableToStringCall")
    override fun valueFromDB(value: Any): Map<String, String> = if (value is Map<*, *>) {
        value as Map<String, String>
    } else {
        error("Unexpected value type for HSTORE column: ${value::class.simpleName}")
    }

    override fun notNullValueToDB(value: Map<String, String>): PGobject = PGobject().apply {
        type = sqlType()
        this.value = HStoreConverter.toString(value)
    }

    override fun valueToString(value: Map<String, String>?): String = "'${super.valueToString(value)}'"
}
