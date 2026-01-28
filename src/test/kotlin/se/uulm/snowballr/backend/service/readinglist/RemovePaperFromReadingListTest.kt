package se.uulm.snowballr.backend.service.readinglist

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class RemovePaperFromReadingListTest : MainServiceTest() {
    @Test
    fun `When the user removes a paper from their reading list, then the request is forwarded correctly`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        mockCurrentUser(user)
        coEvery { paperRepoMock.ensurePaperExists(paperId) } just Runs
        coEvery { readingListRepoMock.removeReadingListEntry(user.id, paperId) } returns Unit

        assertDoesNotThrow { mainService.removePaperFromReadingList(paperId) }
        coVerify(exactly = 1) { readingListRepoMock.removeReadingListEntry(user.id, paperId) }
    }

    @Test
    fun `When the user removes a non-existent paper on their reading list, then a PaperNotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paperId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { paperRepoMock.ensurePaperExists(paperId) } throws PaperNotFoundException(paperId)

            assertThrows<PaperNotFoundException> {
                mainService.removePaperFromReadingList(paperId)
            }
        }
}
