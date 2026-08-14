package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import java.util.UUID

class CreatePaperTest : PaperServiceTest() {
    @Test
    fun `When a paper is created, then the created paper has the correct values`() = runTest {
        val externalIds = listOf(DataBuilder.createExampleExternalId())
        val paper = DataBuilder.createExamplePaper(title = "Test Paper Title", externalIds = externalIds)
        val request = CreatePaperRequest.fromPaper(paper)

        coEvery { paperRepoMock.doesPaperExistByExternalIds(request.externalIds) } returns false
        coEvery { paperRepoMock.createPaper(request) } returns paper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()

        val result = service.createPaper(request)

        assertPaperEquality(paper, result)
    }

    @Test
    fun `When a paper is created with an existent external ID, then a DuplicatePaperException is thrown`() = runTest {
        val externalId = ExternalId(ExternalIdType.DOI, "existing-external-id")
        val request = CreatePaperRequest.fromPaper(DataBuilder.createExamplePaper(externalIds = listOf(externalId)))

        coEvery { paperRepoMock.doesPaperExistByExternalIds(request.externalIds) } returns true

        assertThrows<DuplicatePaperException> { service.createPaper(request) }
    }

    @Test
    fun `When a paper is created without external IDs, then it is not checked whether a paper without external IDs already exists`() =
        runTest {
            val paperId = UUID.randomUUID()
            val request = CreatePaperRequest.fromPaper(DataBuilder.createExamplePaper(externalIds = emptyList()))

            coEvery { paperRepoMock.createPaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

            service.createPaper(request)
            coVerify(exactly = 0) { paperRepoMock.doesPaperExistByExternalIds(request.externalIds) }
        }

    @Test
    fun `When a paper is created with messily formatted data, then the normalized data is persisted`() = runTest {
        val paperId = UUID.randomUUID()
        val paper = DataBuilder.createExamplePaper(title = "  Messy   Title  ", externalIds = emptyList())
        val request = CreatePaperRequest.fromPaper(paper)
        val normalizedRequest = request.copy(title = "Messy Title")

        coEvery { paperRepoMock.createPaper(normalizedRequest) } returns DataBuilder.createExamplePaper(id = paperId)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        service.createPaper(request)

        coVerify(exactly = 1) { paperRepoMock.createPaper(normalizedRequest) }
    }
}
