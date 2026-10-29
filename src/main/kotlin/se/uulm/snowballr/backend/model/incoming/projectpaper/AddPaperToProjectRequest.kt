package se.uulm.snowballr.backend.model.incoming.projectpaper

import java.util.UUID

data class AddPaperToProjectRequest(
    val paperId: UUID,
    val stage: Int,
)
