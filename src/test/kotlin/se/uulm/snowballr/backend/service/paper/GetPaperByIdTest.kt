package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import java.util.UUID

class GetPaperByIdTest : PaperServiceTest() {
    @Test
    fun `When fetching the paper fails, then a TestSpecificException is thrown`() = runTest {
        val paperId = UUID.randomUUID()

        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getPaperById(paperId) }
    }

    @Test
    fun `When a paper is retrieved successfully, then the correct values are returned`() = runTest {
        val author = DataBuilder.createExampleAuthor()
        val paper = DataBuilder.createExamplePaper(authors = listOf(author))

        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        val result = service.getPaperById(paper.id)

        assertPaperEquality(paper, result)
    }
}
