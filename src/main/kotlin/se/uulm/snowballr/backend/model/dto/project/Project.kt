package se.uulm.snowballr.backend.model.dto.project

import com.fasterxml.jackson.annotation.JsonIgnore
import se.uulm.snowballr.backend.table.ProjectTable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectTable].
 */
data class Project(
    val id: UUID,
    val name: String,
    val status: ProjectStatus,
    val currentStage: Int,
    val maxStage: Int,
    val currentStageStartedAt: OffsetDateTime,
    val settings: ProjectSettings,
    @JsonIgnore
    val createdAt: OffsetDateTime,
    @JsonIgnore
    val createdBy: UUID,
    @JsonIgnore
    val modifiedAt: OffsetDateTime?,
    @JsonIgnore
    val modifiedBy: UUID?,
    @JsonIgnore
    val deletedAt: OffsetDateTime?,
    @JsonIgnore
    val deletedBy: UUID?,
    @JsonIgnore
    val archivedAt: OffsetDateTime?,
    @JsonIgnore
    val archivedBy: UUID?,
) {
    /**
     * Checks whether the project is active.
     *
     * A project is considered active if its status is either [ProjectStatus.ACTIVE] or [ProjectStatus.ACTIVE_LOCKED].
     */
    @get:JsonIgnore
    val isActive get() = this.status == ProjectStatus.ACTIVE || this.status == ProjectStatus.ACTIVE_LOCKED

    /**
     * Checks whether the project is deleted.
     *
     * A project is considered deleted if its status is [ProjectStatus.DELETED].
     */
    @get:JsonIgnore
    val isDeleted get() = this.status == ProjectStatus.DELETED
}
