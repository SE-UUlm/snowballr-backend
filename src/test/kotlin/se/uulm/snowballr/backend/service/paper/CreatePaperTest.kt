package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.PaperOuterClass
import java.util.UUID

class CreatePaperTest : MainServiceTest() {
    @Test
    fun `When a paper is created, then no exception is thrown`() = runTest {
        val request = PaperOuterClass.Paper.getDefaultInstance()
        val paperId = UUID.randomUUID()

        coEvery { paperRepoMock.createPaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paperId) } returns emptyList()
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { mainService.createPaper(request) }
    }
}
