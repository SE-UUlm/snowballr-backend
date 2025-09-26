package se.uulm.snowballr.backend.service.projectpaper

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
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetProjectPaperByIdTest : MainServiceTest() {
    private val projectPaperId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id.newBuilder().setId(projectPaperId.toString()).build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(projectPaperRepoMock::getProjectPaperById),
        Arguments.of(paperRepoMock::getPaperById),
    )

    @Suppress("LongMethod", "ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?, isUserAdmin: Boolean) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (isUserAdmin) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            paperId = paper.id,
        )
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val author = DataBuilder.createExampleAuthor()
        val review = DataBuilder.createExampleReview()

        mockCurrentUser(currentUser)

        if (failAt == projectPaperRepoMock::getProjectPaperById) {
            coEvery {
                projectPaperRepoMock.getProjectPaperById(projectPaper.id)
            } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)

        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            if (isUserAdmin) {
                emptyList()
            } else {
                listOf(projectMember)
            }

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.failure(TestSpecificException())
            return
        }
        coEvery { paperRepoMock.getPaperById(projectPaper.paperId) } returns Result.success(paper)

        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
        coEvery { reviewRepoMock.getAllReviewsForProjectPaper(projectPaper.id) } returns listOf(review)
        coEvery {
            reviewHasCriterionRepoMock.getSelectedCriteriaIdsForReviewById(review.id)
        } returns listOf(UUID.randomUUID())
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then a TestSpecificException is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt, true)

        assertThrows<TestSpecificException> {
            mainService.getProjectPaperById(getExampleRequest())
        }
    }

    @Test
    fun `When a server admin retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, true)

        assertDoesNotThrow { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a project member retrieves the project paper, then no exception is thrown`() = runTest {
        mockHappyPathUntil(null, false)

        assertDoesNotThrow { mainService.getProjectPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a non project member retrieves the project paper, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val paper = DataBuilder.createExamplePaper()
        val projectPaper = DataBuilder.createExampleProjectPaper(
            id = projectPaperId,
            projectId = project.id,
            paperId = paper.id,
        )

        mockCurrentUser(currentUser)
        coEvery { projectPaperRepoMock.getProjectPaperById(projectPaper.id) } returns Result.success(projectPaper)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getProjectPaperById(getExampleRequest()) }
    }
}
