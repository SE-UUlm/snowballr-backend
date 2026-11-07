package se.uulm.snowballr.backend.repository

import java.sql.SQLException

// For more information on SQL states see https://www.postgresql.org/docs/16/errcodes-appendix.html

const val UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE = "23505"
const val FOREIGN_KEY_CONSTRAINT_VIOLATION_SQL_STATE = "23503"

/**
 * Checks whether this [SQLException] is thrown due to a unique constraint violation.
 */
fun SQLException.isUniqueConstraintViolation() = this.sqlState == UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE

/**
 * Checks whether this [SQLException] is thrown due to a foreign key constraint violation.
 */
fun SQLException.isForeignKeyConstraintViolation() = this.sqlState == FOREIGN_KEY_CONSTRAINT_VIOLATION_SQL_STATE
