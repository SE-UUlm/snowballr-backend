package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.DataBuilder.toGrpcId
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class RemovePaperFromReadingListTest : MainServiceTest() {
    @Test
    fun `When the user removes a paper from their reading list, then the request is forwarded correctly`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        mockCurrentUser(user)
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { readingListRepoMock.removeReadingListEntry(user.id, paperId) } returns Unit

        assertDoesNotThrow { mainService.removePaperFromReadingList(paperId.toGrpcId()) }
        coVerify(exactly = 1) { readingListRepoMock.removeReadingListEntry(user.id, paperId) }
    }

    @Test
    fun `When the user removes a non-existent paper on their reading list, then a NotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paperId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

            assertThrows<NotFoundException> {
                mainService.removePaperFromReadingList(paperId.toGrpcId())
            }
        }
}
