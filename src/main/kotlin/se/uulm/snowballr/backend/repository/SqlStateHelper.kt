package se.uulm.snowballr.backend.repository

import java.sql.SQLException

/**
 * Checks whether this [SQLException] is thrown due to a unique constraint violation.
 */
fun SQLException.isUniqueConstraintViolation() = this.sqlState == "23505"
