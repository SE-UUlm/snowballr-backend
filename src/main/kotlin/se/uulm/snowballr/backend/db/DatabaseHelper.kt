package se.uulm.snowballr.backend.db

import org.jetbrains.exposed.sql.Transaction

/**
 * Helper for the [Database].
 */
object DatabaseHelper {
    fun Transaction.addExtensions() {
        exec("CREATE EXTENSION IF NOT EXISTS hstore;")
    }
}
