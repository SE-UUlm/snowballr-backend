package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.fetcher.FetcherMap
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
    val similarityThreshold: Float,
    val snowballingType: SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val reviewDecisionMatrix: ReviewDecisionMatrix,
    val fetchers: FetcherMap,
    val currentStageStartedAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val createdBy: UUID,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
    val deletedAt: OffsetDateTime?,
    val deletedBy: UUID?,
    val archivedAt: OffsetDateTime?,
    val archivedBy: UUID?,
)

/**
 * Checks whether the project is active.
 *
 * A project is considered active if its status is either [ProjectStatus.ACTIVE] or [ProjectStatus.ACTIVE_LOCKED].
 */
fun Project.isActive() = this.status == ProjectStatus.ACTIVE || this.status == ProjectStatus.ACTIVE_LOCKED

/**
 * Checks whether the project is deleted.
 *
 * A project is considered deleted if its status is [ProjectStatus.DELETED].
 */
fun Project.isDeleted() = this.status == ProjectStatus.DELETED
