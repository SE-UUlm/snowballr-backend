package se.uulm.snowballr.backend.service.paper

import com.google.protobuf.ByteString
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.PaperOuterClass
import java.util.UUID

class SetPaperPdfTest : MainServiceTest() {
    private val paperId = UUID.randomUUID()
    private val pdfId = UUID.randomUUID()
    private val pdfData = "Test PDF content".toByteArray()

    private fun getExampleRequest(includeData: Boolean = true) = PaperOuterClass.Paper.PdfUpdate
        .newBuilder()
        .setPaperId(paperId.toString())
        .apply {
            if (includeData) {
                pdf = Base.Blob.newBuilder()
                    .setData(ByteString.copyFrom(pdfData))
                    .build()
            }
        }
        .build()

    @Test
    fun `When fetching the paper fails, then a TestSpecificException is thrown`() = runTest {
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

        assertThrows<NotFoundException> { mainService.setPaperPdf(getExampleRequest()) }
    }

    @Test
    fun `When setting PDF for a paper without existing PDF, then no exception is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = null)
        val newPdf = DataBuilder.createExamplePdf(id = pdfId, data = pdfData)
        val updatedPaper = paper.copy(pdfId = pdfId)

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)
        coEvery { pdfRepoMock.createPdf(pdfData) } returns newPdf
        coEvery { paperRepoMock.updatePaperPdfId(paperId, pdfId) } returns updatedPaper

        assertDoesNotThrow { mainService.setPaperPdf(getExampleRequest()) }

        coVerify { pdfRepoMock.createPdf(pdfData) }
        coVerify { paperRepoMock.updatePaperPdfId(paperId, pdfId) }
    }

    @Test
    fun `When setting PDF for a paper with existing PDF, then old PDF is deleted`() = runTest {
        val oldPdfId = UUID.randomUUID()
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = oldPdfId)
        val newPdf = DataBuilder.createExamplePdf(id = pdfId, data = pdfData)
        val updatedPaper = paper.copy(pdfId = pdfId)

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)
        coEvery { pdfRepoMock.deletePdfById(oldPdfId) } returns Unit
        coEvery { pdfRepoMock.createPdf(pdfData) } returns newPdf
        coEvery { paperRepoMock.updatePaperPdfId(paperId, pdfId) } returns updatedPaper

        assertDoesNotThrow { mainService.setPaperPdf(getExampleRequest()) }

        coVerify { pdfRepoMock.deletePdfById(oldPdfId) }
        coVerify { pdfRepoMock.createPdf(pdfData) }
        coVerify { paperRepoMock.updatePaperPdfId(paperId, pdfId) }
    }

    @Test
    fun `When removing PDF from a paper, then PDF is deleted and reference is removed`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = pdfId)
        val updatedPaper = paper.copy(pdfId = null)

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)
        coEvery { pdfRepoMock.deletePdfById(pdfId) } returns Unit
        coEvery { paperRepoMock.updatePaperPdfId(paperId, null) } returns updatedPaper

        assertDoesNotThrow { mainService.setPaperPdf(getExampleRequest(includeData = false)) }

        coVerify { pdfRepoMock.deletePdfById(pdfId) }
        coVerify { paperRepoMock.updatePaperPdfId(paperId, null) }
    }

    @Test
    fun `When an error occurs during PDF creation, then a TestSpecificException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = paperId, pdfId = null)

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { paperRepoMock.getPaperById(paperId) } returns Result.success(paper)
        coEvery { pdfRepoMock.createPdf(pdfData) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.setPaperPdf(getExampleRequest()) }
    }
}
