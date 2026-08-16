package se.uulm.snowballr.backend.model.outgoing.project

import java.time.OffsetDateTime

data class ProjectInformation(
    /**
     * Value in range [0, 1] that represents the project progress.
     */
    val progress: Float,
    /**
     * The timestamp when the project was created.
     */
    val creationDate: OffsetDateTime,
    /**
     * The timestamp when the last stage in the project was started.
     */
    val lastStageStarted: OffsetDateTime,
)
