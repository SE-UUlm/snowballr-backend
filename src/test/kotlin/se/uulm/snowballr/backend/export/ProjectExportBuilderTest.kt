package se.uulm.snowballr.backend.export

import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import java.util.UUID
import kotlin.test.assertEquals

class ProjectExportBuilderTest {
    @Test
    fun `When selected criteria contains unknown ids, then unknown is exported for those entries`() {
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMemberWithUser()
        val criterion = DataBuilder.createExampleProjectCriterion()
        val unknownCriterionId = UUID.randomUUID()
        val projectPaper = DataBuilder.createExampleProjectPaperFull(
            reviewsWithSelectedCriteria = listOf(
                DataBuilder.createExampleReviewWithSelectedCriteriaIds(
                    review = DataBuilder.createExampleReview(userId = projectMember.user.id),
                    selectedCriteriaIds = listOf(criterion.id, unknownCriterionId),
                ),
            ),
        )

        val export = ProjectExportBuilder(
            project = project,
            projectMembers = listOf(projectMember),
            projectPapers = listOf(projectPaper),
            projectCriteria = listOf(criterion),
        ).buildExport()

        val selectedCriteriaIds = export
            .stages.single()
            .papers.single()
            .reviews.single()
            .selectedCriteriaIds

        assertEquals(listOf("0", "unknown"), selectedCriteriaIds)
    }
}
