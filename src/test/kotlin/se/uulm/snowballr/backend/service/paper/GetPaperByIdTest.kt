package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPaperByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    @Test
    fun `When parsing the paper ID fails, then an InvalidIdException is thrown`() = runTest {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()

        assertThrows<InvalidIdException> { mainService.getPaperById(request) }
    }

    @Test
    fun `When fetching the paper fails, then a TestSpecificException is thrown`() = runTest {
        coEvery { paperRepoMock.getPaperById(any()) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getPaperById(getExampleRequest()) }
    }

    @Test
    fun `When a paper is retrieved, then no error is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val author = DataBuilder.createExampleAuthor()

        coEvery { paperRepoMock.getPaperById(paper.id) } returns Result.success(paper)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())

        assertDoesNotThrow { mainService.getPaperById(getExampleRequest()) }
    }
}
