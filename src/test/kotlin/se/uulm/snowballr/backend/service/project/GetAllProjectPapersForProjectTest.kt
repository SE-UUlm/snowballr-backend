package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
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
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllProjectPapersForProjectTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestId.toString()).build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(GrpcContext::getUserIdFromContext),
        Arguments.of(userRepoMock::getUserById),
        Arguments.of(projectRepoMock::doesProjectExistById),
        Arguments.of(projectMemberRepoMock::getProjectMembers),
        Arguments.of(projectPaperRepoMock::getAllProjectPapersWithPapers),
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
        val project = DataBuilder.createExampleProject(id = requestId)
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val projectPaper = DataBuilder.createExampleProjectPaper(projectId = project.id, paperId = paper.id)
        val projectPaperWithPaper = ProjectPaperWithPaper(projectPaper, paper)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        if (failAt == GrpcContext::getUserIdFromContext) {
            every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
            return
        }
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id

        if (failAt == userRepoMock::getUserById) {
            coEvery { userRepoMock.getUserById(currentUser.id) } throws TestSpecificException()
            return
        }
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

        if (failAt == projectRepoMock::doesProjectExistById) {
            coEvery { projectRepoMock.doesProjectExistById(any()) } throws TestSpecificException()
            return
        }
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true

        if (failAt == projectMemberRepoMock::getProjectMembers) {
            coEvery { projectMemberRepoMock.getProjectMembers(any()) } throws TestSpecificException()
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (failAt == projectPaperRepoMock::getAllProjectPapersWithPapers) {
            coEvery {
                projectPaperRepoMock.getAllProjectPapersWithPapers(any())
            } throws TestSpecificException()
            return
        }
        coEvery {
            projectPaperRepoMock.getAllProjectPapersWithPapers(project.id)
        } returns listOf(projectPaperWithPaper)

        if (failAt == authorOfPaperRepoMock::getAuthorsOfPaperById) {
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(any()) } throws TestSpecificException()
            return
        }
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)

        if (failAt == citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(any())
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        if (failAt == reviewRepoMock::getAllReviewsForProjectPaper) {
            coEvery { reviewRepoMock.getAllReviewsForProjectPaper(any()) } throws TestSpecificException()
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
    fun `When a step fails, then an exception is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt, true)
        assertThrows<TestSpecificException> {
            mainService.getAllProjectPapersForProject(getExampleRequest())
        }
    }

    @Test
    fun `When a server admin requests the project papers, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)
        assertDoesNotThrow { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a project member requests the project papers, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)
        assertDoesNotThrow { mainService.getAllProjectPapersForProject(getExampleRequest()) }
    }

    @Test
    fun `When a non project member requests the project papers, then an unauthorized exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = requestId)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException> {
            mainService.getAllProjectPapersForProject(
                getExampleRequest(),
            )
        }
    }
}
