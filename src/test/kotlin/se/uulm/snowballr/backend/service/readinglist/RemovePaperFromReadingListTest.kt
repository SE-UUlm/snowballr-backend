package se.uulm.snowballr.backend.service.readinglist

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import java.util.UUID

class RemovePaperFromReadingListTest : ReadingListServiceTest() {
    @Test
    fun `When a user removes a paper from their reading list, then the request is forwarded correctly`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        mockCurrentUser(user)
        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coJustRun { readingListRepoMock.removeReadingListEntry(user.id, paperId) }

        assertDoesNotThrow { service.removePaperFromReadingList(paperId) }
        coVerify(exactly = 1) { readingListRepoMock.removeReadingListEntry(user.id, paperId) }
    }

    @Test
    fun `When a user removes a non-existent paper on their reading list, then a PaperNotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paperId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

            assertThrows<PaperNotFoundException> {
                service.removePaperFromReadingList(paperId)
            }
        }
}
