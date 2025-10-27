package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID

class GetPaperPdfTest : MainServiceTest() {
    private val paperId = UUID.randomUUID()
    private val pdfId = UUID.randomUUID()
    private val pdfData = "Test PDF content".toByteArray()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(paperId.toString())
        .build()

    @Test
    fun `When fetching the paper fails, then a NotFoundException is thrown`() = runTest {
        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.failure(
            NotFoundException(
                se.uulm.snowballr.backend.model.EntityType.PAPER,
                paperId.toString(),
            ),
        )

        assertThrows<NotFoundException> { mainService.getPaperPdf(getExampleRequest()) }
    }

    @Test
    fun `When paper has no PDF, then a FailedPreconditionException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = null)

        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)

        assertThrows<FailedPreconditionException> { mainService.getPaperPdf(getExampleRequest()) }
    }

    @Test
    fun `When PDF is retrieved successfully, then no exception is thrown and correct data is returned`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = pdfId)
        val pdf = DataBuilder.createExamplePdf(id = pdfId, data = pdfData)

        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)
        coEvery { pdfRepoMock.getPdfById(pdfId) } returns Result.success(pdf)

        val result = assertDoesNotThrow { mainService.getPaperPdf(getExampleRequest()) }

        assertThat(result.data.toByteArray()).isEqualTo(pdfData)
    }

    @Test
    fun `When fetching the PDF fails, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = pdfId)

        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)
        coEvery { pdfRepoMock.getPdfById(pdfId) } returns Result.failure(
            NotFoundException(
                se.uulm.snowballr.backend.model.EntityType.PDF,
                pdfId.toString(),
            ),
        )

        assertThrows<NotFoundException> { mainService.getPaperPdf(getExampleRequest()) }
    }
}
