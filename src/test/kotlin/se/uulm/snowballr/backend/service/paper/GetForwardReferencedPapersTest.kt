package se.uulm.snowballr.backend.service.paper

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class GetForwardReferencedPapersTest : MainServiceTest() {
    @Test
    fun `When the paper doesn't exist, then a PaperNotFoundException is thrown`() = runTest {
        val paperId = UUID.randomUUID()

        coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

        assertThrows<PaperNotFoundException> {
            mainService.getForwardReferencedPapers(paperId)
        }
    }

    @Test
    fun `When one of the forward references doesn't exist, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper()
        val forwardReferenceId = UUID.randomUUID()

        coEvery { paperRepoMock.ensurePaperExists(paper.id) } just Runs
        coEvery {
            citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(forwardReferenceId)
        coEvery { paperRepoMock.getPaperById(forwardReferenceId) } throws PaperNotFoundException(paper.id)

        assertThrows<NotFoundException> {
            mainService.getForwardReferencedPapers(paper.id)
        }
    }

    @Test
    fun `When the forward references of an existent paper are retrieved successfully, then no exception is thrown`() =
        runTest {
            val author = DataBuilder.createExampleAuthor()
            val paper = DataBuilder.createExamplePaper(authors = listOf(author))
            val forwardReferenceId = UUID.randomUUID()
            val citingPaper = DataBuilder.createExamplePaper(id = forwardReferenceId)

            coEvery { paperRepoMock.ensurePaperExists(paper.id) } just Runs
            coEvery {
                citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
            } returns listOf(forwardReferenceId)
            coEvery { paperRepoMock.getPaperById(forwardReferenceId) } returns Result.success(citingPaper)
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(forwardReferenceId)
            } returns listOf(UUID.randomUUID())

            assertDoesNotThrow { mainService.getForwardReferencedPapers(paper.id) }
        }
}
