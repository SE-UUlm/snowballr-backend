package se.uulm.snowballr.backend.model.incoming.project

enum class ProjectInfoField(val grpcPath: String) {
    PROJECT_PROGRESS("project_progress"),
    CREATION_DATE("creation_date"),
    LAST_STAGE_STARTED("last_stage_started"),
}
