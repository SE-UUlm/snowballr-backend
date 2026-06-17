package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import snowballr.PaperOuterClass
import java.util.UUID

class CreatePaperTest : PaperServiceTest() {
    @Test
    fun `When a paper is created, then no exception is thrown`() = runTest {
        val request = PaperOuterClass.Paper.newBuilder()
            .setExternalId("new-external-id")
            .build()
        val paperId = UUID.randomUUID()

        coEvery { paperRepoMock.doesPaperExistByExternalId(request.externalId) } returns false
        coEvery { paperRepoMock.createPaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { service.createPaper(request) }
    }

    @Test
    fun `When a paper is created with an existent external ID, then a DuplicatePaperException is thrown`() = runTest {
        val request = PaperOuterClass.Paper.newBuilder()
            .setExternalId("existing-external-id")
            .build()

        coEvery { paperRepoMock.doesPaperExistByExternalId(request.externalId) } returns true

        assertThrows<DuplicatePaperException> { service.createPaper(request) }
    }

    @Test
    fun `When a paper is created with an empty external ID, then it is not checked whether a paper with an empty external ID already exists`() =
        runTest {
            val paperId = UUID.randomUUID()
            val request = PaperOuterClass.Paper.newBuilder()
                .setExternalId("")
                .build()

            coEvery { paperRepoMock.createPaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

            service.createPaper(request)
            coVerify(exactly = 0) { paperRepoMock.doesPaperExistByExternalId(request.externalId) }
        }
}
