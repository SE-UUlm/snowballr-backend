package se.uulm.snowballr.backend.export

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.ProjectExport
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.MemberRole
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class ProjectExportManagerTest {
    companion object {
        @JvmStatic
        fun supportedFormats() = ProjectExportManager.getSupportedFormats().map { Arguments.of(it.name) }
    }

    @Nested
    inner class GetSupportedFormats {
        @Test
        fun `When the supported formats are requested, then the set of formats is not empty`() {
            val formats = ProjectExportManager.getSupportedFormats()

            assertThat(formats).isNotEmpty()
        }
    }

    @Nested
    inner class ExportProject {
        @ParameterizedTest(name = "When exporting a project in the {0} format, then returned data is not empty")
        @MethodSource("se.uulm.snowballr.backend.export.ProjectExportManagerTest#supportedFormats")
        fun `When exporting a project in a format, then returned data is not empty`(format: ExportFormat) {
            val project = DataBuilder.createExampleProject(name = "Exported Project")
            val projectMembers = listOf(DataBuilder.createExampleProjectMemberWithUser())
            val projectPapers = listOf(DataBuilder.createExampleProjectPaperFull())
            val projectCriteria = listOf(DataBuilder.createExampleProjectCriterion())

            val fileExport =
                ProjectExportManager.exportProject(format, project, projectMembers, projectPapers, projectCriteria)

            assertThat(fileExport.data).isNotEmpty()
            val filename = fileExport.filename
            assertThat(filename).startsWith("Exported_Project-")
            assertThat(filename.split('.').last()).isNotEmpty
        }

        @Test
        @Suppress("LongMethod")
        fun `When exporting a project with members and papers, then exported bytes are returned`() {
            val exampleDateTime = OffsetDateTime.of(2023, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC)
            val exampleDateTimeString = "2023-03-15T10:00Z"

            val project = DataBuilder.createExampleProject(
                maxStage = 2,
                createdAt = exampleDateTime,
            )
            val projectMembers = listOf(
                DataBuilder.createExampleProjectMemberWithUser(
                    projectMember = DataBuilder.createExampleProjectMember(role = MemberRole.MEMBER_ROLE_DEFAULT),
                ),
                DataBuilder.createExampleProjectMemberWithUser(
                    projectMember = DataBuilder.createExampleProjectMember(role = MemberRole.MEMBER_ROLE_ADMIN),
                ),
                DataBuilder.createExampleProjectMemberWithUser(
                    projectMember = DataBuilder.createExampleProjectMember(role = MemberRole.MEMBER_ROLE_DEFAULT),
                ),
            )
            val projectCriteria = listOf(
                DataBuilder.createExampleProjectCriterion(
                    category = CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION,
                ),
                DataBuilder.createExampleProjectCriterion(
                    category = CriterionCategory.CRITERION_CATEGORY_EXCLUSION,
                ),
                DataBuilder.createExampleProjectCriterion(
                    category = CriterionCategory.CRITERION_CATEGORY_INCLUSION,
                ),
            )
            val projectPapers = listOf(
                DataBuilder.createExampleProjectPaperFull(
                    projectPaper = DataBuilder.createExampleProjectPaper(stage = 0),
                    paper = DataBuilder.createExamplePaper(
                        externalId = null,
                        createdAt = exampleDateTime,
                        modifiedAt = null,
                    ),
                    reviewsWithSelectedCriteria = listOf(
                        DataBuilder.createExampleReviewWithSelectedCriteriaIds(
                            review = DataBuilder.createExampleReview(
                                decision = ReviewDecision.REVIEW_DECISION_ACCEPTED,
                                userId = projectMembers[0].user.id,
                            ),
                            selectedCriteriaIds = listOf(projectCriteria[0].id),
                        ),
                    ),
                ),
                DataBuilder.createExampleProjectPaperFull(
                    projectPaper = DataBuilder.createExampleProjectPaper(stage = 1),
                    paper = DataBuilder.createExamplePaper(createdAt = exampleDateTime, modifiedAt = null),
                    reviewsWithSelectedCriteria = listOf(
                        DataBuilder.createExampleReviewWithSelectedCriteriaIds(
                            review = DataBuilder.createExampleReview(
                                decision = ReviewDecision.REVIEW_DECISION_MAYBE,
                                userId = projectMembers[1].user.id,
                            ),
                            selectedCriteriaIds = listOf(projectCriteria[1].id),
                        ),
                    ),
                ),
                DataBuilder.createExampleProjectPaperFull(
                    projectPaper = DataBuilder.createExampleProjectPaper(stage = 2),
                    paper = DataBuilder.createExamplePaper(createdAt = exampleDateTime, modifiedAt = exampleDateTime),
                    reviewsWithSelectedCriteria = listOf(
                        DataBuilder.createExampleReviewWithSelectedCriteriaIds(
                            review = DataBuilder.createExampleReview(
                                decision = ReviewDecision.REVIEW_DECISION_DECLINED,
                                userId = UUID.randomUUID(),
                            ),
                            selectedCriteriaIds = listOf(projectCriteria[2].id),
                        ),
                    ),
                ),
            )

            val format = ExportFormat.JSON
            val fileExport =
                ProjectExportManager.exportProject(format, project, projectMembers, projectPapers, projectCriteria)

            val projectExport = Json.decodeFromString<ProjectExport>(String(fileExport.data))

            assertThat(projectExport.name).isEqualTo(project.name)
            assertThat(projectExport.members).hasSize(projectMembers.size)
            assertThat(projectExport.stages).hasSize(3)
            assertThat(projectExport.criteria).hasSize(projectCriteria.size)
            assertEquals(exampleDateTimeString, projectExport.createdAt)

            val projectMembersExport = projectExport.members
            projectMembers.forEachIndexed { index, member ->
                val memberExport = projectMembersExport[index]
                assertEquals(index.toString(), memberExport.id)
                assertEquals(member.user.firstName, memberExport.firstName)
                assertEquals(member.user.lastName, memberExport.lastName)
                assertEquals(member.user.email, memberExport.email)
                assertEquals(member.projectMember.role, memberExport.role)
            }

            val projectStagesExport = projectExport.stages
            projectStagesExport.forEachIndexed { stageIndex, stageExport ->
                assertEquals(stageIndex.toString(), stageExport.id)
                assertThat(stageExport.papers).hasSize(1)
                val paperInStage = projectPapers[stageIndex]
                val paperExport = stageExport.papers[0]
                assertEquals(paperInStage.paper.title, paperExport.title)
                assertEquals(paperInStage.paper.externalId.orEmpty(), paperExport.externalId)
                assertEquals(paperInStage.paper.abstract, paperExport.abstract)
                assertEquals(paperInStage.paper.year, paperExport.year)
                assertEquals(paperInStage.paper.publisher, paperExport.publisher)
                assertEquals(paperInStage.paper.publicationType, paperExport.publicationType)
                assertEquals(paperInStage.paper.publicationName, paperExport.publicationName)
                assertThat(paperExport.authors).hasSize(paperInStage.paper.authors.size)
                paperInStage.paper.authors.forEachIndexed { authorIndex, author ->
                    val authorExport = paperExport.authors[authorIndex]
                    assertEquals("${author.firstName} ${author.lastName}", authorExport)
                }
                assertThat(paperExport.reviews).hasSize(paperInStage.reviewsWithSelectedCriteria.size)
                paperInStage.reviewsWithSelectedCriteria.forEachIndexed { reviewIndex, reviewWithSelectedCriteriaIds ->
                    val reviewExport = paperExport.reviews[reviewIndex]
                    val projectMemberIndex = stageIndex.toString()
                    val review = reviewWithSelectedCriteriaIds.review
                    if (stageIndex != 2) {
                        assertEquals(projectMemberIndex, reviewExport.reviewerId)
                    } else {
                        // Reviews by unknown reviewers get "unknown" as reviewer ID
                        assertEquals("unknown", reviewExport.reviewerId)
                    }
                    assertEquals(review.decision, reviewExport.decision)

                    val selectedCriteriaIds = reviewWithSelectedCriteriaIds.selectedCriteriaIds
                    assertThat(reviewExport.selectedCriteriaIds).hasSize(selectedCriteriaIds.size)
                    selectedCriteriaIds.forEachIndexed { criterionIdIndex, criterionId ->
                        val criterionIdExport = reviewExport.selectedCriteriaIds[criterionIdIndex]
                        val expectedCriterionId = projectCriteria.indexOfFirst { it.id == criterionId }
                        assertEquals("$expectedCriterionId", criterionIdExport)
                    }
                }
                assertEquals(paperInStage.projectPaper.decision, paperExport.finalDecision)
                assertEquals(exampleDateTimeString, paperExport.createdAt)
                if (stageIndex == 2) {
                    assertEquals(exampleDateTimeString, paperExport.modifiedAt)
                } else {
                    assertEquals("", paperExport.modifiedAt)
                }
            }

            val projectCriteriaExport = projectExport.criteria
            projectCriteria.forEachIndexed { index, criterion ->
                val criterionExport = projectCriteriaExport[index]
                assertEquals(index.toString(), criterionExport.id)
                assertEquals(criterion.tag, criterionExport.tag)
                assertEquals(criterion.name, criterionExport.name)
                assertEquals(criterion.description, criterionExport.description)
                assertEquals(criterion.category, criterionExport.category)
            }
        }
    }
}
