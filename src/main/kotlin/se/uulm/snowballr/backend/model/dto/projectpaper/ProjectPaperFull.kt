package se.uulm.snowballr.backend.model.dto.projectpaper

import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds

/**
 * DTO representing a project paper along with its associated paper and reviews with selected criteria.
 *
 * @property projectPaper The project paper information.
 * @property paper The associated paper information.
 * @property reviewsWithSelectedCriteria A list of reviews along with their selected criteria.
 */
data class ProjectPaperFull(
    val projectPaper: ProjectPaper,
    val paper: Paper,
    val reviewsWithSelectedCriteria: List<ReviewWithSelectedCriteriaIds>,
)
