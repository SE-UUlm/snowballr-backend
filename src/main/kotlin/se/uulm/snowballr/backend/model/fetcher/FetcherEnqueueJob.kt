package se.uulm.snowballr.backend.model.fetcher

import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaper
import java.util.UUID

/**
 * Enqueuing job for fetching referenced papers from a passed origin paper.
 */
data class FetcherEnqueueJob(
    val projectPaper: ProjectPaper,
    val triggeringUserId: UUID,
)
