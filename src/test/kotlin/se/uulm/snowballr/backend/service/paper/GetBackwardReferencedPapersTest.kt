package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
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
    fun `When the paper doesn't exist, then a NotFoundException is thrown`() = runTest {
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false
        assertThrows<NotFoundException> {
            mainService.getBackwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When one of the backward references doesn't exist, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId)
        val backwardReferenceId = UUID.randomUUID()

        coEvery { paperRepoMock.doesPaperExistById(paper.id) } returns true
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(backwardReferenceId)
        coEvery {
            paperRepoMock.getPaperById(backwardReferenceId)
        } throws NotFoundException(entityType = EntityType.PAPER, paperId.toString())

        assertThrows<NotFoundException> {
            mainService.getBackwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When the backward references of an existent paper are retrieved successfully, then no exception is thrown`() =
        runTest {
            val paper = DataBuilder.createExamplePaper(id = paperId)
            val backwardReferenceId = UUID.randomUUID()
            val referencedPaper = DataBuilder.createExamplePaper(id = backwardReferenceId)

            coEvery { paperRepoMock.doesPaperExistById(paper.id) } returns true
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
