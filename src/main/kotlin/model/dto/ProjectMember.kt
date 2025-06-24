package se.uulm.snowballr.backend.model.dto

import org.jetbrains.exposed.dao.id.CompositeID
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectMemberTable].
 */
data class ProjectMember(
    val id: CompositeID,
    val projectId: UUID,
    val userId: UUID,
    val role: ProjectOuterClass.MemberRole,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
)
