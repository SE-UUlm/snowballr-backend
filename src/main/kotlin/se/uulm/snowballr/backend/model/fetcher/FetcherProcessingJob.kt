package se.uulm.snowballr.backend.model.fetcher

import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import java.util.UUID

/**
 * Processing job for fetching referenced papers.
 */
data class FetcherProcessingJob(
    val projectId: UUID,
    val fetchers: FetcherMap,
    val snowballingType: SnowballingType,
    val targetStage: Long,
    val paperId: UUID,
    val triggeringUserId: UUID,
)
