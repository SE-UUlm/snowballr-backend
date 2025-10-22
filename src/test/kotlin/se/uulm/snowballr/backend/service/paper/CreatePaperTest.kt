package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.PaperOuterClass
import java.util.UUID

class CreatePaperTest : MainServiceTest() {
    @Test
    fun `When a paper is created, then no exception is thrown`() = runTest {
        val request = PaperOuterClass.Paper.getDefaultInstance()
        val paperId = UUID.randomUUID()

        coEvery { paperRepoMock.doesPaperExistByExternalId(request.externalId) } returns false
        coEvery { paperRepoMock.createPaper(request) } returns DataBuilder.createExamplePaper(id = paperId)
        coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paperId) } returns emptyList()

        assertDoesNotThrow { mainService.createPaper(request) }
    }

    @Test
    fun `When a paper is created with an existent external ID, then a DuplicateEntityException is thrown`() = runTest {
        val request = PaperOuterClass.Paper.newBuilder()
            .setExternalId("existing-external-id")
            .build()

        coEvery { paperRepoMock.doesPaperExistByExternalId(request.externalId) } returns true

        assertThrows<DuplicateEntityException> { mainService.createPaper(request) }
    }
}
