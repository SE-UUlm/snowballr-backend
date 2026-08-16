package se.uulm.snowballr.backend.model.incoming.project

import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import java.util.UUID

data class UpdateProjectRequest(
    val projectId: UUID,
    val name: String,
    val status: ProjectStatus,
    val settings: UpdateProjectSettingRequest,
) {
    companion object {
        fun fromProject(project: Project) = UpdateProjectRequest(
            projectId = project.id,
            name = project.name,
            status = project.status,
            settings = UpdateProjectSettingRequest.fromProject(project),
        )

        fun fromProjectResponse(project: ProjectResponse) = UpdateProjectRequest(
            projectId = project.id,
            name = project.name,
            status = project.status,
            settings = UpdateProjectSettingRequest.fromProjectResponse(project),
        )
    }
}
