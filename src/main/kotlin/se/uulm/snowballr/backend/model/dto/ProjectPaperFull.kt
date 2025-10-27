package se.uulm.snowballr.backend.model.dto

data class ProjectPaperFull(
    val projectPaper: ProjectPaper,
    val paper: Paper,
    val reviews: List<Review>,
)
