package se.uulm.snowballr.backend.service.paper

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest
import java.util.UUID

class UpdatePaperTest : PaperServiceTest() {
    private val paperId = UUID.randomUUID()

    private fun getExampleRequest() = UpdatePaperRequest.fromPaper(
        DataBuilder.createExamplePaper(
            id = paperId,
            title = "Updated Title",
            abstract = "Updated Abstract",
            externalId = "10.1000/updateddoi",
            year = 2023,
        ),
    )

    @Test
    fun `When an existent paper is updated, then the updated paper is returned`() = runTest {
        val request = getExampleRequest()
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val examplePaper = DataBuilder.createExamplePaper(id = paperId, authors = listOf(exampleAuthor))

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.getPaperByExternalIds(request.paper.externalId) } returns Result.failure(Exception())
        coEvery { paperRepoMock.updatePaper(request) } returns examplePaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        val result = service.updatePaper(request, emptyList())

        assertPaperEquality(examplePaper, result)
    }

    @Test
    fun `When a non-existent paper is updated, then a PaperNotFoundException is thrown`() = runTest {
        val request = getExampleRequest()

        coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

        assertThrows<PaperNotFoundException> {
            service.updatePaper(request, emptyList())
        }
    }

    @Test
    fun `When a paper is updated with an external ID that already exists, then a DuplicateEntityException is thrown`() =
        runTest {
            val externalId = "10.1000/existingdoi"
            val request = getExampleRequest().copy(externalId = externalId)
            val existingPaperWithSameExternalId = DataBuilder.createExamplePaper(
                id = UUID.randomUUID(),
                externalId = externalId,
            )

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery { paperRepoMock.getPaperByExternalIds("10.1000/existingdoi") } returns Result.success(
                existingPaperWithSameExternalId,
            )

            assertThrows<DuplicatePaperException> { service.updatePaper(request, emptyList()) }
        }

    @Test
    fun `When a paper is updated with an external ID that belongs to itself, then the updated paper is returned`() =
        runTest {
            val request = getExampleRequest()
            val exampleAuthor = DataBuilder.createExampleAuthor()
            val existingPaperWithSameExternalId = DataBuilder.createExamplePaper(
                id = paperId,
                externalId = request.externalId,
                authors = listOf(exampleAuthor),
            )

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery { paperRepoMock.getPaperByExternalIds(request.paper.externalId) } returns
                Result.success(existingPaperWithSameExternalId)
            coEvery { paperRepoMock.updatePaper(request, emptyList()) } returns existingPaperWithSameExternalId
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

            val result = service.updatePaper(request, emptyList())

            assertPaperEquality(existingPaperWithSameExternalId, result)
        }

    @Test
    fun `When a paper is updated without external ID, then duplicate lookup by external ID is skipped`() = runTest {
        val request = getExampleRequest().copy(externalId = null)
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val updatedPaper = DataBuilder.createExamplePaper(
            id = paperId,
            externalId = "",
            authors = listOf(exampleAuthor),
        )

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.updatePaper(request, emptyList()) } returns updatedPaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        service.updatePaper(request)
        coVerify(exactly = 0) { paperRepoMock.getPaperByExternalIds(any()) }
    }
}
