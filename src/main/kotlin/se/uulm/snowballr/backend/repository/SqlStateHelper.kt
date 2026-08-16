package se.uulm.snowballr.backend.repository

import java.sql.SQLException

const val UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE = "23505"

/**
 * Checks whether this [SQLException] is thrown due to a unique constraint violation.
 */
fun SQLException.isUniqueConstraintViolation() = this.sqlState == UNIQUE_CONSTRAINT_VIOLATION_SQL_STATE
