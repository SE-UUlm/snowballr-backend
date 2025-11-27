package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.DataBuilder.toGrpcId
import se.uulm.snowballr.backend.model.exception.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import java.util.UUID

class IsPaperOnReadingListTest : MainServiceTest() {
    @Test
    fun `When the user checks if a paper is on their reading list, then the request is forwarded correctly`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2")

            mockCurrentUser(user)
            coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paper1.id) } returns true
            coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paper2.id) } returns false
            coEvery { paperRepoMock.doesPaperExistById(paper1.id) } returns true
            coEvery { paperRepoMock.doesPaperExistById(paper2.id) } returns true

            assertTrue(mainService.isPaperOnReadingList(paper1.id.toGrpcId()).value)
            coVerify(exactly = 1) { readingListRepoMock.isPaperOnReadingList(user.id, paper1.id) }

            assertFalse(mainService.isPaperOnReadingList(paper2.id.toGrpcId()).value)
            coVerify(exactly = 1) { readingListRepoMock.isPaperOnReadingList(user.id, paper2.id) }
        }

    @Test
    fun `When the user checks if a non-existent paper is on their reading list, then a NotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paperId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

            assertThrows<NotFoundException> {
                mainService.isPaperOnReadingList(paperId.toGrpcId())
            }
        }
}
