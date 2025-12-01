package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
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

class GetForwardReferencedPapersTest : MainServiceTest() {
    private val paperId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(paperId.toString())
        .build()

    @Test
    fun `When the paper doesn't exist, then a NotFoundException is thrown`() = runTest {
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false
        assertThrows<NotFoundException> {
            mainService.getForwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When one of the forward references doesn't exist, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId)
        val forwardReferenceId = UUID.randomUUID()

        coEvery { paperRepoMock.doesPaperExistById(paper.id) } returns true
        coEvery {
            citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(forwardReferenceId)
        coEvery { paperRepoMock.getPaperById(forwardReferenceId) } throws PaperNotFoundException(paper.id)

        assertThrows<NotFoundException> {
            mainService.getForwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When the forward references of an existent paper are retrieved successfully, then no exception is thrown`() =
        runTest {
            val author = DataBuilder.createExampleAuthor()
            val paper = DataBuilder.createExamplePaper(id = paperId, authors = listOf(author))
            val forwardReferenceId = UUID.randomUUID()
            val citingPaper = DataBuilder.createExamplePaper(id = forwardReferenceId)

            coEvery { paperRepoMock.doesPaperExistById(paper.id) } returns true
            coEvery {
                citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
            } returns listOf(forwardReferenceId)
            coEvery { paperRepoMock.getPaperById(forwardReferenceId) } returns Result.success(citingPaper)
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(forwardReferenceId)
            } returns listOf(UUID.randomUUID())

            assertDoesNotThrow { mainService.getForwardReferencedPapers(getExampleRequest()) }
        }
}
