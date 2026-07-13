package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import java.util.UUID

class CreatePaperTest : PaperServiceTest() {
    @Test
    fun `When a paper is created, then the created paper has the correct values`() = runTest {
        val externalId = "new-external-id"
        val paper = DataBuilder.createExamplePaper(title = "Test Paper Title", externalId = externalId)
        val request = CreatePaperRequest.fromPaper(paper)

        coEvery { paperRepoMock.doesPaperExistByExternalId(externalId) } returns false
        coEvery { paperRepoMock.createPaper(request) } returns paper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id) } returns emptyList()

        val result = service.createPaper(request)

        assertPaperEquality(paper, result)
    }

    @Test
    fun `When a paper is created with an existent external ID, then a DuplicatePaperException is thrown`() = runTest {
        val externalId = "existent-external-id"
        val request = CreatePaperRequest.fromPaper(DataBuilder.createExamplePaper(externalId = externalId))

        coEvery { paperRepoMock.doesPaperExistByExternalId(externalId) } returns true

        assertThrows<DuplicatePaperException> { service.createPaper(request) }
    }

    @Test
    fun `When a paper is created without an external ID, then it is not checked whether a paper with an external ID already exists`() =
        runTest {
            val paperId = UUID.randomUUID()
            val request = CreatePaperRequest.fromPaper(DataBuilder.createExamplePaper(externalId = null))

            coEvery { paperRepoMock.createPaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

            service.createPaper(request)
            coVerify(exactly = 0) { paperRepoMock.doesPaperExistByExternalId(any()) }
        }
}
