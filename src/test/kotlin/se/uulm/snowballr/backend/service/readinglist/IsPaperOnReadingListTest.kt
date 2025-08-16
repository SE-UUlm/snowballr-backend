package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.DataBuilder.toGrpcId
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsPaperOnReadingListTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        val paperId = Base.Id.getDefaultInstance()
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
        assertThrows<TestSpecificException> { mainService.isPaperOnReadingList(paperId) }
    }

    @Test
    fun `When checking the reading list status fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paperId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.isPaperOnReadingList(paperId.toGrpcId()) }
    }

    @Test
    fun `When the user checks if a paper is on their reading list, then the request is forwarded correctly`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2")

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { paperRepoMock.doesPaperExistById(paper1.id) } returns true
            coEvery { paperRepoMock.doesPaperExistById(paper2.id) } returns true
            coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paper1.id) } returns true
            coEvery { readingListRepoMock.isPaperOnReadingList(user.id, paper2.id) } returns false

            assertTrue(mainService.isPaperOnReadingList(paper1.id.toGrpcId()).value)
            assertFalse(mainService.isPaperOnReadingList(paper2.id.toGrpcId()).value)
            coVerify(exactly = 1) { readingListRepoMock.isPaperOnReadingList(user.id, paper1.id) }
            coVerify(exactly = 1) { readingListRepoMock.isPaperOnReadingList(user.id, paper2.id) }
        }

    @Test
    fun `When the user checks if a non-existent paper is on their reading list, then an exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paperId = UUID.randomUUID()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

            assertThrows<NotFoundException> {
                mainService.isPaperOnReadingList(paperId.toGrpcId())
            }
        }
}
