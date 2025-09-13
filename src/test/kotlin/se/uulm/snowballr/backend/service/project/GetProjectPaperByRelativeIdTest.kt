package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.Project
import snowballr.UserOuterClass
import java.util.UUID
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetProjectPaperByRelativeIdTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val localPaperId = Random.nextLong()

    private fun getExampleRequest() = Project.Paper.Get
        .newBuilder()
        .setProjectId(projectId.toString())
        .setRelativeProjectPaperId(localPaperId.toString())
        .build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectRepoMock::doesProjectExistById),
        Arguments.of(projectPaperRepoMock::getProjectPaperByRelativeId),
        Arguments.of(projectMemberRepoMock::getProjectMembers),
        Arguments.of(paperRepoMock::getPaperById),
        Arguments.of(authorOfPaperRepoMock::getAuthorsOfPaperById),
        Arguments.of(citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById),
        Arguments.of(reviewRepoMock::getAllReviewsForProjectPaper),
        Arguments.of(reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById),
    )

    @Suppress("LongMethod", "ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserOuterClass.UserRole.USER_ROLE_ADMIN
            } else {
                UserOuterClass.UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            localPaperId = localPaperId,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        if (failAt == projectRepoMock::doesProjectExistById) {
            coEvery { projectRepoMock.doesProjectExistById(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true

        if (failAt == projectPaperRepoMock::getProjectPaperByRelativeId) {
            coEvery {
                projectPaperRepoMock.getProjectPaperByRelativeId(project.id, projectPaper.localPaperId)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, projectPaper.localPaperId)
        } returns Result.success(projectPaper)

        mockCurrentUser(currentUser)

        if (failAt == projectMemberRepoMock::getProjectMembers) {
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } throws TestSpecificException()
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } throws TestSpecificException()
            return
        }
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.success(paper)

        if (failAt == authorOfPaperRepoMock::getAuthorsOfPaperById) {
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } throws TestSpecificException()
            return
        }
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)

        if (failAt == citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        if (failAt == reviewRepoMock::getAllReviewsForProjectPaper) {
            coEvery {
                reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id)
            } throws TestSpecificException()
            return
        }
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)

        if (failAt == reviewHasCriterionRepoMock::getSelectedCriteriaIdsForReviewById) {
            coEvery {
                reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(any())
            } throws TestSpecificException()
            return
        }
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt, true)
        assertThrows<TestSpecificException> {
            mainService.getProjectPaperByRelativeId(getExampleRequest())
        }
    }

    @Test
    fun `When a server admin retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)
        assertDoesNotThrow { mainService.getProjectPaperByRelativeId(getExampleRequest()) }
    }

    @Test
    fun `When a project member retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)
        assertDoesNotThrow { mainService.getProjectPaperByRelativeId(getExampleRequest()) }
    }

    @Test
    fun `When the project to retrieve the project paper from doesn't exist, then a NotFoundException is thrown`() =
        runTest {
            coEvery {
                projectRepoMock.doesProjectExistById(projectId)
            } throws NotFoundException(EntityType.PROJECT, projectId.toString())

            assertThrows<NotFoundException> {
                mainService.getProjectPaperByRelativeId(getExampleRequest())
            }
        }

    @Test
    fun `When a non project member retrieves the project paper, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(projectId)
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            projectId = project.id,
            paperId = paper.id,
            localPaperId = localPaperId,
        )

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery {
            projectPaperRepoMock.getProjectPaperByRelativeId(project.id, projectPaper.localPaperId)
        } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> {
            mainService.getProjectPaperByRelativeId(getExampleRequest())
        }
    }
}
