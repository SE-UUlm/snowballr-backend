package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID

class GetPaperByIdTest : MainServiceTest() {
    private val paperId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(paperId.toString())
        .build()

    @Test
    fun `When fetching the paper fails, then a TestSpecificException is thrown`() = runTest {
        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a paper is retrieved successfully, then no exception is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId)
        val author = DataBuilder.createExampleAuthor()

        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getPaperById(getExampleRequest()) }
    }
}
