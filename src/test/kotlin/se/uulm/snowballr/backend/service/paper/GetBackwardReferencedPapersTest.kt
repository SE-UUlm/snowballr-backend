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
import snowballr.Base
import java.util.UUID

class GetBackwardReferencedPapersTest : MainServiceTest() {
    private val paperId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(paperId.toString())
        .build()

    @Test
    fun `When the paper doesn't exist, then a PaperNotFoundException is thrown`() = runTest {
        coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)
        assertThrows<PaperNotFoundException> {
            mainService.getBackwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When one of the backward references doesn't exist, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId)
        val backwardReferenceId = UUID.randomUUID()

        coEvery { paperRepoMock.ensurePaperExists(paper.id) } just Runs
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(backwardReferenceId)
        coEvery { paperRepoMock.getPaperById(backwardReferenceId) } throws PaperNotFoundException(paperId)

        assertThrows<NotFoundException> {
            mainService.getBackwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When the backward references of an existent paper are retrieved successfully, then no exception is thrown`() =
        runTest {
            val author = DataBuilder.createExampleAuthor()
            val paper = DataBuilder.createExamplePaper(id = paperId, authors = listOf(author))
            val backwardReferenceId = UUID.randomUUID()
            val referencedPaper = DataBuilder.createExamplePaper(id = backwardReferenceId)

            coEvery { paperRepoMock.ensurePaperExists(paper.id) } just Runs
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } returns listOf(backwardReferenceId)
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(backwardReferenceId)
            } returns listOf(UUID.randomUUID())
            coEvery { paperRepoMock.getPaperById(backwardReferenceId) } returns Result.success(referencedPaper)

            assertDoesNotThrow { mainService.getBackwardReferencedPapers(getExampleRequest()) }
        }
}
