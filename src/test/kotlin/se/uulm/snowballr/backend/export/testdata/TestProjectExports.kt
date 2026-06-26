package se.uulm.snowballr.backend.export.testdata

import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.export.CriterionExport
import se.uulm.snowballr.backend.model.export.PaperExport
import se.uulm.snowballr.backend.model.export.PaperReviewExport
import se.uulm.snowballr.backend.model.export.ProjectExport
import se.uulm.snowballr.backend.model.export.ProjectMemberExport
import se.uulm.snowballr.backend.model.export.ProjectStageExport
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.PaperDecision

val emptyProjectExport = ProjectExport(
    name = "Empty Project",
    members = emptyList(),
    stages = emptyList(),
    criteria = emptyList(),
    createdAt = "2023-03-15T10:00:00Z",
)

@Suppress("NamedArguments")
val fullProjectExport = ProjectExport(
    name = "Full Project",
    members = listOf(
        ProjectMemberExport("1", "Alice", "Smith", "alice.smith@example.com", MemberRole.MEMBER_ROLE_DEFAULT),
        ProjectMemberExport("2", "John", "Doe", "john.doe@example.com", MemberRole.MEMBER_ROLE_DEFAULT),
        ProjectMemberExport("3", "Jane", "Doe", "jane.doe@example.com", MemberRole.MEMBER_ROLE_UNSPECIFIED),
    ),
    stages = listOf(
        ProjectStageExport(
            "1",
            papers = listOf(
                PaperExport(
                    "Paper Title 1",
                    "doi/1234",
                    "Example Abstract",
                    2001,
                    "Publisher 1",
                    "Publication Type 1",
                    "Publication Name 1",
                    authors = listOf("Author 1", "Author 2"),
                    reviews = listOf(
                        PaperReviewExport("1", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("0")),
                        PaperReviewExport("2", ReviewDecision.REVIEW_DECISION_DECLINED, listOf("1")),
                        PaperReviewExport("3", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("2")),
                    ),
                    finalDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
                    createdAt = "2023-03-15T10:00:00Z",
                    modifiedAt = "",
                ),
                PaperExport(
                    "Paper Title 2",
                    "doi/5678",
                    "Example Abstract 2",
                    2002,
                    "Publisher 2",
                    "Publication Type 2",
                    "Publication Name 2",
                    authors = listOf("Author 3", "Author 4"),
                    reviews = listOf(
                        PaperReviewExport("1", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("0")),
                        PaperReviewExport("2", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("1")),
                        PaperReviewExport("3", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("2")),
                    ),
                    finalDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
                    createdAt = "2023-03-15T10:00:00Z",
                    modifiedAt = "",
                ),
            ),
        ),
        ProjectStageExport(
            "2",
            papers = listOf(
                PaperExport(
                    "Paper Title 3",
                    "doi/9876",
                    "Example Abstract 3",
                    2003,
                    "Publisher 3",
                    "Publication Type 3",
                    "Publication Name 3",
                    authors = listOf("Author 5", "Author 6"),
                    reviews = listOf(
                        PaperReviewExport("1", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("0")),
                        PaperReviewExport("2", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("1")),
                        PaperReviewExport("3", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("2")),
                    ),
                    finalDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
                    createdAt = "2023-03-15T10:00:00Z",
                    modifiedAt = "",
                ),
                PaperExport(
                    "Paper Title 4",
                    "doi/0123",
                    "Example Abstract 4",
                    2004,
                    "Publisher 4",
                    "Publication Type 4",
                    "Publication Name 4",
                    authors = listOf("Author 7", "Author 8"),
                    reviews = listOf(
                        PaperReviewExport("1", ReviewDecision.REVIEW_DECISION_DECLINED, listOf("0")),
                        PaperReviewExport("2", ReviewDecision.REVIEW_DECISION_DECLINED, listOf("1")),
                        PaperReviewExport("3", ReviewDecision.REVIEW_DECISION_DECLINED, listOf("2")),
                    ),
                    finalDecision = PaperDecision.PAPER_DECISION_DECLINED,
                    createdAt = "2023-03-15T10:00:00Z",
                    modifiedAt = "2025-03-15T10:00:00Z",
                ),
            ),
        ),
        ProjectStageExport(
            "3",
            papers = listOf(
                PaperExport(
                    "Paper Title 5",
                    "doi/4321",
                    "Example Abstract 5",
                    2005,
                    "Publisher 5",
                    "Publication Type 5",
                    "Publication Name 5",
                    authors = listOf("Author 9", "Author 10"),
                    reviews = listOf(
                        PaperReviewExport("1", ReviewDecision.REVIEW_DECISION_ACCEPTED, listOf("0")),
                    ),
                    finalDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
                    createdAt = "2023-03-15T10:00:00Z",
                    modifiedAt = "",
                ),
                PaperExport(
                    "Paper Title 6",
                    "doi/6543",
                    "Example Abstract 6",
                    2006,
                    "Publisher 6",
                    "Publication Type 6",
                    "Publication Name 6",
                    authors = listOf("Author 11", "Author 12"),
                    reviews = listOf(
                        PaperReviewExport("1", ReviewDecision.REVIEW_DECISION_MAYBE, listOf("0")),
                        PaperReviewExport("2", ReviewDecision.REVIEW_DECISION_DECLINED, listOf("1")),
                    ),
                    finalDecision = PaperDecision.PAPER_DECISION_DECLINED,
                    createdAt = "2023-03-15T10:00:00Z",
                    modifiedAt = "2024-03-15T10:00:00Z",
                ),
            ),
        ),
    ),
    criteria = listOf(
        CriterionExport(
            id = "0",
            tag = "C1",
            name = "Criterion 1",
            description = "Description for Criterion 1",
            category = CriterionCategory.CRITERION_CATEGORY_INCLUSION,
        ),
        CriterionExport(
            id = "1",
            tag = "C2",
            name = "Criterion 2",
            description = "Description for Criterion 2",
            category = CriterionCategory.CRITERION_CATEGORY_EXCLUSION,
        ),
        CriterionExport(
            id = "2",
            tag = "C3",
            name = "Criterion 3",
            description = "Description for Criterion 3",
            category = CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION,
        ),
    ),
    createdAt = "2021-03-15T10:00:00Z",
)
