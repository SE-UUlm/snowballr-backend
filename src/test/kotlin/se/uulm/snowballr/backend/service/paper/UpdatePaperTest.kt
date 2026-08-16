package se.uulm.snowballr.backend.service.paper

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.paper.PaperField
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
            externalIds = listOf(ExternalId(ExternalIdType.DOI, "10.1000/updateddoi")),
            year = 2023,
        ),
    )

    @Test
    fun `When an existent paper is updated, then the updated paper is returned`() = runTest {
        val request = getExampleRequest()
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val examplePaper = DataBuilder.createExamplePaper(id = paperId, authors = listOf(exampleAuthor))

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.updatePaper(request, emptySet()) } returns examplePaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        val result = service.updatePaper(request, emptySet())

        assertPaperEquality(examplePaper, result)

        coVerify(exactly = 0) { paperRepoMock.getPapersByExternalIds(any()) }
    }

    @Test
    fun `When a non-existent paper is updated, then a PaperNotFoundException is thrown`() = runTest {
        val request = getExampleRequest()

        coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

        assertThrows<PaperNotFoundException> {
            service.updatePaper(request, emptySet())
        }
    }

    @Test
    fun `When a paper is updated with an external ID that already exists, then a DuplicateEntityException is thrown`() =
        runTest {
            val externalIds = listOf(ExternalId(ExternalIdType.DOI, "10.1000/existingdoi"))
            val request = getExampleRequest().copy(externalIds = externalIds)
            val existingPaperWithSameExternalId = DataBuilder.createExamplePaper(
                id = UUID.randomUUID(),
                externalIds = externalIds,
            )

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery {
                paperRepoMock.getPapersByExternalIds(externalIds)
            } returns listOf(existingPaperWithSameExternalId)

            assertThrows<DuplicatePaperException> { service.updatePaper(request, setOf(PaperField.EXTERNAL_IDS)) }
        }

    @Test
    fun `When a paper is updated with external IDs that already exist, then a DuplicateEntityException is thrown`() =
        runTest {
            val externalIds = listOf(
                ExternalId(ExternalIdType.DOI, "10.1000/existingdoi"),
                ExternalId(ExternalIdType.ACL, "1234567890"),
            )
            val request = getExampleRequest().copy(externalIds = externalIds)
            val existingPapers = listOf(
                DataBuilder.createExamplePaper(id = UUID.randomUUID(), externalIds = listOf(externalIds[0])),
                DataBuilder.createExamplePaper(id = UUID.randomUUID(), externalIds = listOf(externalIds[1])),
            )

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery { paperRepoMock.getPapersByExternalIds(externalIds) } returns existingPapers

            assertThrows<DuplicatePaperException> { service.updatePaper(request, setOf(PaperField.EXTERNAL_IDS)) }
        }

    @Test
    fun `When a paper is updated with an external ID but the fields doesn't contain the external IDs, then existing papers are not checked`() =
        runTest {
            val request = getExampleRequest().copy(externalIds = emptyList())
            val examplePaper = DataBuilder.createExamplePaper(id = paperId)

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery { paperRepoMock.updatePaper(request, setOf(PaperField.EXTERNAL_IDS)) } returns examplePaper
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

            service.updatePaper(request, setOf(PaperField.EXTERNAL_IDS))

            coVerify(exactly = 0) { paperRepoMock.getPapersByExternalIds(any()) }
        }

    @Test
    fun `When a paper is updated with an external ID that belongs to itself, then the updated paper is returned`() =
        runTest {
            val request = getExampleRequest()
            val exampleAuthor = DataBuilder.createExampleAuthor()
            val existingPaperWithSameExternalId = DataBuilder.createExamplePaper(
                id = paperId,
                externalIds = request.externalIds,
                authors = listOf(exampleAuthor),
            )

            coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
            coEvery { paperRepoMock.getPapersByExternalIds(request.externalIds) } returns
                listOf(existingPaperWithSameExternalId)
            coEvery {
                paperRepoMock.updatePaper(request, setOf(PaperField.EXTERNAL_IDS))
            } returns existingPaperWithSameExternalId
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

            val result = service.updatePaper(request, setOf(PaperField.EXTERNAL_IDS))

            assertPaperEquality(existingPaperWithSameExternalId, result)
        }

    @Test
    fun `When a paper is updated without external ID, then duplicate lookup by external ID is skipped`() = runTest {
        val request = getExampleRequest().copy(externalIds = emptyList())
        val exampleAuthor = DataBuilder.createExampleAuthor()
        val updatedPaper = DataBuilder.createExamplePaper(
            id = paperId,
            externalIds = emptyList(),
            authors = listOf(exampleAuthor),
        )

        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { paperRepoMock.updatePaper(request, emptySet()) } returns updatedPaper
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        service.updatePaper(request, emptySet())
        coVerify(exactly = 0) { paperRepoMock.getPapersByExternalIds(any()) }
    }
}
