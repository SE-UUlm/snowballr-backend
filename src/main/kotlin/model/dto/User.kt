package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.UserTable
import snowballr.UserOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [UserTable].
 */
data class User(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserOuterClass.UserRole,
    val status: UserOuterClass.UserStatus,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
    val deletedAt: OffsetDateTime?,
)
