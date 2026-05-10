package se.uulm.snowballr.backend.service.readinglist

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import java.util.UUID

class IsPaperOnReadingListTest : ReadingListServiceTest() {
    @Test
    fun `When a user checks if a paper is on their reading list, then the request is forwarded correctly`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paper1 = DataBuilder.createExamplePaper()
        val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2")

        mockCurrentUser(user)
        coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paper1.id) } returns true
        coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paper2.id) } returns false
        coEvery { paperRepoMock.ensurePaperExists(paper1.id) } just Runs
        coEvery { paperRepoMock.ensurePaperExists(paper2.id) } just Runs

        assertTrue(service.isPaperOnReadingList(paper1.id))
        coVerify(exactly = 1) { readingListRepoMock.isPaperOnReadingList(user.id, paper1.id) }

        assertFalse(service.isPaperOnReadingList(paper2.id))
        coVerify(exactly = 1) { readingListRepoMock.isPaperOnReadingList(user.id, paper2.id) }
    }

    @Test
    fun `When a user checks if a non-existent paper is on their reading list, then a PaperNotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paperId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

            assertThrows<PaperNotFoundException> {
                service.isPaperOnReadingList(paperId)
            }
        }
}
