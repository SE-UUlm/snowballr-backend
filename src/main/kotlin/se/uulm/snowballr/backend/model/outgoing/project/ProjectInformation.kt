package se.uulm.snowballr.backend.model.outgoing.project

import com.google.protobuf.timestamp
import snowballr.ProjectOuterClass
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

fun ProjectInformation.toGrpc(): ProjectOuterClass.Project.Information =
    ProjectOuterClass.Project.Information.newBuilder()
        .setProjectProgress(progress)
        .setCreationDate(timestamp { seconds = creationDate.toEpochSecond() })
        .setLastStageStarted(timestamp { seconds = lastStageStarted.toEpochSecond() })
        .build()
