package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateProjectPaperException
import se.uulm.snowballr.backend.model.exception.invalidargument.StageOutOfRangeException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.parseUUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper
import snowballr.ReviewOuterClass.Review as GrpcReview

class ProjectPaperIntegrationTest : IntegrationTest() {
    private suspend fun reviewPaper(projectPaper: GrpcProjectPaper, decision: ReviewDecision) =
        reviewService.createReview(
            GrpcReview.Create.newBuilder()
                .setProjectPaperId(projectPaper.id)
                .setDecision(decision.toGrpc())
                .build(),
        )

    private suspend fun setNumberOfRequiredReviewers(project: Project, numberOfReviewers: Int): Project {
        val projectUpdate = project.copy(
            reviewDecisionMatrix = project.reviewDecisionMatrix.copy(numberOfReviewers = numberOfReviewers),
        )

        return projectService.updateProject(
            UpdateProjectRequest.fromProject(projectUpdate),
            setOf("project.settings.decision_matrix.number_of_reviewers"),
        )
    }

    @Nested
    inner class AddPaperToProject {
        @Test
        fun `When the same paper is added to a project twice, then a DuplicateProjectPaperException is thrown`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val paper = createPaper()

                addToProject(project, paper)

                assertThrows<DuplicateProjectPaperException> { addToProject(project, paper) }
            }

        @Test
        fun `When a paper is added with a stage above maxStage, then a StageOutOfRangeException is thrown`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val paper = createPaper()

            assertThrows<StageOutOfRangeException> {
                projectPaperService.addPaperToProject(
                    GrpcProjectPaper.Add.newBuilder()
                        .setProjectId(project.id.toString())
                        .setPaperId(paper.id)
                        .setStage(1) // maxStage is 0 by default
                        .build(),
                )
            }
        }

        @Test
        fun `When a non-admin project member tries to add a paper, then access is denied`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val otherUser = addUser(DataBuilder.createExampleUser(email = "member.user@example.com"))

            inviteUserToProject(project, otherUser, acceptInvitation = true)

            val paper = createPaper()

            actAsUser(otherUser.id) {
                assertThrows<UnauthorizedCreateException> { addToProject(project, paper) }
            }
        }
    }

    @Nested
    inner class GetProjectPaperByRelativeId {
        @Test
        fun `When a paper is added to a project, then it can be retrieved by its relative ID`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val added = addToProject(project, createPaper())

            val fetched = projectPaperService.getProjectPaperByRelativeId(
                GrpcProjectPaper.Get.newBuilder()
                    .setProjectId(project.id.toString())
                    .setRelativeProjectPaperId(added.localId)
                    .build(),
            )

            assertEquals(added.id, fetched.id)
        }

        @Test
        fun `When multiple papers are added, then each can be retrieved by its own relative ID`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val ppA = addToProject(project, createPaper("A"))
            val ppB = addToProject(project, createPaper("B"))

            val fetchedA = projectPaperService.getProjectPaperByRelativeId(
                GrpcProjectPaper.Get.newBuilder()
                    .setProjectId(project.id.toString())
                    .setRelativeProjectPaperId(ppA.localId)
                    .build(),
            )
            val fetchedB = projectPaperService.getProjectPaperByRelativeId(
                GrpcProjectPaper.Get.newBuilder()
                    .setProjectId(project.id.toString())
                    .setRelativeProjectPaperId(ppB.localId)
                    .build(),
            )

            assertEquals(ppA.id, fetchedA.id)
            assertEquals(ppB.id, fetchedB.id)
        }
    }

    @Nested
    inner class GetPapersToReviewForProject {
        @Test
        fun `When no papers have been added, then the papers-to-review list is empty`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))

            val toReview = projectPaperService.getPapersToReviewForProject(project.id)

            assertTrue(toReview.projectPapersList.isEmpty())
        }

        @Test
        fun `When a paper has not been reviewed, then it appears in the papers-to-review list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val pp = addToProject(project, createPaper())

            val toReview = projectPaperService.getPapersToReviewForProject(project.id)

            assertTrue(toReview.projectPapersList.any { it.id == pp.id })
        }

        @Test
        fun `When the current user has reviewed a paper, then it no longer appears in the to-review list`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val pp = addToProject(project, createPaper())

            reviewPaper(pp, ReviewDecision.ACCEPTED)

            val toReview = projectPaperService.getPapersToReviewForProject(project.id)

            assertFalse(toReview.projectPapersList.any { it.id == pp.id })
        }

        @Test
        fun `When another reviewer gives a paper a final decision, then it does not appear in the to-review list`() =
            runTest {
                var project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                project = setNumberOfRequiredReviewers(project, 1)
                val otherUser = addUser(DataBuilder.createExampleUser(email = "other.reviewer@example.com"))
                inviteUserToProject(project, otherUser, acceptInvitation = true)
                val pp = addToProject(project, createPaper())

                // With the default decision matrix (0 reviewers required) any single review
                // immediately sets a final decision.
                actAsUser(otherUser.id) {
                    reviewPaper(pp, ReviewDecision.DECLINED)
                }

                val toReview = projectPaperService.getPapersToReviewForProject(project.id)

                assertFalse(toReview.projectPapersList.any { it.id == pp.id })
            }

        @Test
        fun `When one of two papers is reviewed, then only the unreviewed paper appears in the to-review list`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val ppA = addToProject(project, createPaper("A"))
                val ppB = addToProject(project, createPaper("B"))

                reviewPaper(ppA, ReviewDecision.ACCEPTED)

                val toReview = projectPaperService.getPapersToReviewForProject(project.id)

                assertFalse(toReview.projectPapersList.any { it.id == ppA.id })
                assertTrue(toReview.projectPapersList.any { it.id == ppB.id })
            }
    }

    @Nested
    inner class Navigation {
        @Test
        fun `When three papers are added, then getNextPaper returns the paper immediately after the given one`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val ppA = addToProject(project, createPaper("A"))
                val ppB = addToProject(project, createPaper("B"))
                addToProject(project, createPaper("C"))
                val ppAId = parseUUID(ppA.id, EntityType.PROJECT_PAPER)

                val next = projectPaperService.getNextPaper(ppAId)

                assertEquals(ppB.id, next.id)
            }

        @Test
        fun `When three papers are added, then getPreviousPaper returns the paper immediately before the given one`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                addToProject(project, createPaper("A"))
                val ppB = addToProject(project, createPaper("B"))
                val ppC = addToProject(project, createPaper("C"))
                val ppCId = parseUUID(ppC.id, EntityType.PROJECT_PAPER)

                val previous = projectPaperService.getPreviousPaper(ppCId)

                assertEquals(ppB.id, previous.id)
            }

        @Test
        fun `When there is no next paper, then getNextPaper throws a FailedPreconditionException`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val ppA = addToProject(project, createPaper())
            val ppAId = parseUUID(ppA.id, EntityType.PROJECT_PAPER)

            assertThrows<FailedPreconditionException> { projectPaperService.getNextPaper(ppAId) }
        }

        @Test
        fun `When there is no previous paper, then getPreviousPaper throws a FailedPreconditionException`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val ppA = addToProject(project, createPaper())
            val ppAId = parseUUID(ppA.id, EntityType.PROJECT_PAPER)

            assertThrows<FailedPreconditionException> { projectPaperService.getPreviousPaper(ppAId) }
        }
    }

    @Nested
    inner class GetNextPaperToReview {
        @Test
        fun `When there are subsequent unreviewed papers, then getNextPaperToReview returns one of them`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
            val ppA = addToProject(project, createPaper("A"))
            val ppB = addToProject(project, createPaper("B"))
            val ppAId = parseUUID(ppA.id, EntityType.PROJECT_PAPER)

            val next = projectPaperService.getNextPaperToReview(ppAId)

            assertEquals(ppB.id, next.id)
        }

        @Test
        fun `When there are no subsequent papers, then getNextPaperToReview throws FailedPreconditionException`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val ppA = addToProject(project, createPaper())
                val ppAId = parseUUID(ppA.id, EntityType.PROJECT_PAPER)

                assertThrows<FailedPreconditionException> { projectPaperService.getNextPaperToReview(ppAId) }
            }

        @Test
        fun `When the current user has already reviewed all subsequent papers, then getNextPaperToReview throws FailedPreconditionException`() =
            runTest {
                val project = projectService.createProject(CreateProjectRequest(name = "Test Project"))
                val ppA = addToProject(project, createPaper("A"))
                val ppB = addToProject(project, createPaper("B"))
                val ppAId = parseUUID(ppA.id, EntityType.PROJECT_PAPER)

                reviewPaper(ppB, ReviewDecision.ACCEPTED)

                assertThrows<FailedPreconditionException> { projectPaperService.getNextPaperToReview(ppAId) }
            }
    }
}
