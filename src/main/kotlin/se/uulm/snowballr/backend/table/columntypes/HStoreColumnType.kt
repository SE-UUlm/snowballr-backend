package se.uulm.snowballr.backend.table.columntypes

import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

/**
 * Uses PostgreSQL's HSTORE extension to store key-value pairs in a single column.
 */
class HStoreColumnType : TextColumnType() {
    override fun sqlType(): String = "HSTORE"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue: PGobject? = value?.let {
            PGobject().apply {
                type = sqlType()
                this.value = value as? String
            }
        }
        super.setParameter(stmt, index, parameterValue)
    }

    override fun valueToString(value: String?) = "'${super.valueToString(value)}'"
}
