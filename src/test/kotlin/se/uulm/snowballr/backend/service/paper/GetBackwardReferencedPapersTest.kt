package se.uulm.snowballr.backend.service.paper

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import java.util.UUID
import kotlin.test.assertEquals

class GetBackwardReferencedPapersTest : PaperServiceTest() {
    @Test
    fun `When the paper doesn't exist, then a PaperNotFoundException is thrown`() = runTest {
        val paperId = UUID.randomUUID()

        coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)
        assertThrows<PaperNotFoundException> {
            service.getBackwardReferencedPapers(paperId)
        }
    }

    @Test
    fun `When one of the backward references doesn't exist, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper()
        val backwardReferenceId = UUID.randomUUID()

        coEvery { paperRepoMock.ensurePaperExists(paper.id) } just Runs
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(backwardReferenceId)
        coEvery { paperRepoMock.getPaperById(backwardReferenceId) } throws PaperNotFoundException(paper.id)

        assertThrows<NotFoundException> {
            service.getBackwardReferencedPapers(paper.id)
        }
    }

    @Test
    fun `When the backward references of an existent paper are retrieved successfully, then the correct values are returned`() =
        runTest {
            val author = DataBuilder.createExampleAuthor()
            val paper = DataBuilder.createExamplePaper(authors = listOf(author))
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

            val result = service.getBackwardReferencedPapers(paper.id)

            assertEquals(1, result.size)
            val resultElement = result.first()
            assertPaperEquality(referencedPaper, resultElement)
        }
}
