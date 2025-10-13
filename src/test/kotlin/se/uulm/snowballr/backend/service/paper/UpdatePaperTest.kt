package se.uulm.snowballr.backend.service.paper

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID
import snowballr.PaperOuterClass.Author as GrpcAuthor
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

    @Test
    fun `When an existent paper is updated, then no exception is thrown`() = runTest {
        val request = GrpcPaper.Update.newBuilder()
            .setPaper(getExamplePaperBuilder().build())
            .build()
        val examplePaper = DataBuilder.createExamplePaper(id = paperId)
        val exampleAuthor = DataBuilder.createExampleAuthor()

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { paperRepoMock.updatePaper(request) } returns examplePaper
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paperId) } returns listOf(exampleAuthor)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { mainService.updatePaper(request) }
    }

    @Test
    fun `When a non-existent paper is updated, then a NotFoundException is thrown`() = runTest {
        val request = GrpcPaper.Update.newBuilder()
            .setPaper(getExamplePaperBuilder().build())
            .build()

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

        assertThrows<NotFoundException> {
            mainService.updatePaper(request)
        }
    }

    @Test
    fun `When updating a paper contains updating the authors, then the author list is correctly updated`() = runTest {
        val newAuthor1 = GrpcAuthor.newBuilder()
            .setFirstName("Max")
            .setLastName("Mustermann")
            .setOrcid("0000-0002-9079-593X")
            .build()
        val newAuthor1Dto = DataBuilder.createExampleAuthor()
        val newAuthor2 = GrpcAuthor.newBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setOrcid("0000-0001-7195-7801")
            .build()
        val newAuthor2Dto = DataBuilder.createExampleAuthor()
        val newAuthors = listOf(newAuthor1, newAuthor2)
        val paperRequest = getExamplePaperBuilder()
            .addAllAuthors(newAuthors)
            .build()

        val existingAuthor = DataBuilder.createExampleAuthor()
        val request = GrpcPaper.Update.newBuilder()
            .setPaper(paperRequest)
            .setMask(FieldMaskUtil.fromStringList(listOf("paper.authors")))
            .build()

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paperId) } returns listOf(existingAuthor)
        coEvery { authorOfPaperRepoMock.removeAuthorFromPaper(any(), any()) } just runs
        coEvery { authorRepoMock.createAuthor(newAuthor1) } returns newAuthor1Dto
        coEvery { authorRepoMock.createAuthor(newAuthor2) } returns newAuthor2Dto
        coEvery { authorOfPaperRepoMock.addAuthorToPaper(any(), any()) } just runs
        coEvery { paperRepoMock.updatePaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { mainService.updatePaper(request) }

        coVerify(exactly = 1) { authorOfPaperRepoMock.removeAuthorFromPaper(existingAuthor.id, paperId) }
        coVerify(exactly = 1) { authorOfPaperRepoMock.addAuthorToPaper(newAuthor1Dto.id, paperId) }
        coVerify(exactly = 1) { authorOfPaperRepoMock.addAuthorToPaper(newAuthor2Dto.id, paperId) }
    }

    @Test
    fun `When updating a paper contains updating the same authors, then the author list is not updated`() = runTest {
        val newAuthor1 = GrpcAuthor.newBuilder()
            .setFirstName("Max")
            .setLastName("Mustermann")
            .setOrcid("0000-0002-9079-593X")
            .build()
        val newAuthor2 = GrpcAuthor.newBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setOrcid("0000-0001-7195-7801")
            .build()
        val newAuthors = listOf(newAuthor1, newAuthor2)
        val paperRequest = getExamplePaperBuilder()
            .addAllAuthors(newAuthors)
            .build()

        // Same Authors as in update request
        val existingAuthor1 = DataBuilder.createExampleAuthor(orcid = "0000-0002-9079-593X")
        val existingAuthor2 = DataBuilder.createExampleAuthor(firstName = "John", lastName = "Doe")
        val request = GrpcPaper.Update.newBuilder()
            .setPaper(paperRequest)
            .setMask(FieldMaskUtil.fromStringList(listOf("paper.authors")))
            .build()

        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery {
            authorOfPaperRepoMock.getAuthorsOfPaperById(paperId)
        } returns listOf(existingAuthor1, existingAuthor2)
        coEvery { paperRepoMock.updatePaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { mainService.updatePaper(request) }

        coVerify(exactly = 0) { authorOfPaperRepoMock.removeAuthorFromPaper(any(), any()) }
        coVerify(exactly = 0) { authorOfPaperRepoMock.addAuthorToPaper(any(), any()) }
    }
}
