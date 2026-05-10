package se.uulm.snowballr.backend.service.paper

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

class UpdatePaperTest : PaperServiceTest() {
    private val paperId = UUID.randomUUID()

    private fun getExamplePaperBuilder() = GrpcPaper
        .newBuilder()
        .setId(paperId.toString())
        .setTitle("Updated Title")
        .setAbstrakt("Updated Abstract")
        .setExternalId("10.1000/updateddoi")
        .setYear(2023)

    private fun getExampleRequest() = GrpcPaper.Update
        .newBuilder()
        .setPaper(getExamplePaperBuilder().build())
        .build()

    @Test
    fun `When an existent paper is updated, then no exception is thrown`() = runTest {
        val request = getExampleRequest()
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val examplePaper = DataBuilder.createExamplePaper(id = paperId, authors = listOf(exampleAuthor))

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.getPaperByExternalId(request.paper.externalId) } returns Result.failure(Exception())
        coEvery { paperRepoMock.updatePaper(request) } returns examplePaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { service.updatePaper(request) }
    }

    @Test
    fun `When a non-existent paper is updated, then a PaperNotFoundException is thrown`() = runTest {
        val request = getExampleRequest()

        coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

        assertThrows<PaperNotFoundException> {
            service.updatePaper(request)
        }
    }

    @Test
    fun `When a paper is updated with an external ID that already exists, then a DuplicateEntityException is thrown`() =
        runTest {
            val request = GrpcPaper.Update
                .newBuilder()
                .setPaper(
                    getExamplePaperBuilder().setExternalId("10.1000/existingdoi").build(),
                )
                .build()
            val existingPaperWithSameExternalId = DataBuilder.createExamplePaper(
                id = UUID.randomUUID(),
                externalId = "10.1000/existingdoi",
            )

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery { paperRepoMock.getPaperByExternalId("10.1000/existingdoi") } returns Result.success(
                existingPaperWithSameExternalId,
            )

            assertThrows<DuplicatePaperException> { service.updatePaper(request) }
        }

    @Test
    fun `When a paper is updated with an external ID that belongs to itself, then no exception is thrown`() = runTest {
        val request = getExampleRequest()
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val existingPaperWithSameExternalId = DataBuilder.createExamplePaper(
            id = paperId,
            externalId = request.paper.externalId,
            authors = listOf(exampleAuthor),
        )

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.getPaperByExternalId(request.paper.externalId) } returns
            Result.success(existingPaperWithSameExternalId)
        coEvery { paperRepoMock.updatePaper(request) } returns existingPaperWithSameExternalId
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { service.updatePaper(request) }
    }

    @Test
    fun `When a paper is updated without external ID, then duplicate lookup by external ID is skipped`() = runTest {
        val request = GrpcPaper.Update
            .newBuilder()
            .setPaper(getExamplePaperBuilder().setExternalId("").build())
            .build()
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val updatedPaper = DataBuilder.createExamplePaper(
            id = paperId,
            externalId = "",
            authors = listOf(exampleAuthor),
        )

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.updatePaper(request) } returns updatedPaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { service.updatePaper(request) }
        coVerify(exactly = 0) { paperRepoMock.getPaperByExternalId(any()) }
    }
}
