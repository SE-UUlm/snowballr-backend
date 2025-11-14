package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID
import snowballr.PaperOuterClass.Paper as GrpcPaper

class UpdatePaperTest : MainServiceTest() {
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

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { paperRepoMock.doesPaperExistByExternalId(request.paper.externalId!!) } returns false
        coEvery { paperRepoMock.updatePaper(request) } returns examplePaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { mainService.updatePaper(request) }
    }

    @Test
    fun `When a non-existent paper is updated, then a NotFoundException is thrown`() = runTest {
        val request = getExampleRequest()

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

        assertThrows<NotFoundException> {
            mainService.updatePaper(request)
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

            coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
            coEvery { paperRepoMock.doesPaperExistByExternalId(request.paper.externalId!!) } returns true

            assertThrows<SnowballRException.DuplicateEntityException> { mainService.updatePaper(request) }
        }
}
