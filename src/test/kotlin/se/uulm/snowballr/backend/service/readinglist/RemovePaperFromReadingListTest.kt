package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.DataBuilder.toGrpcId
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID

class RemovePaperFromReadingListTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        val paperId = Base.Id.getDefaultInstance()
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
        assertThrows<TestSpecificException> { mainService.removePaperFromReadingList(paperId) }
    }

    @Test
    fun `When removing the reading list entry fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { readingListRepoMock.removeReadingListEntry(user.id, paperId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.removePaperFromReadingList(paperId.toGrpcId()) }
    }

    @Test
    fun `When the user removes a paper from their reading list, then the request is forwarded correctly`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { readingListRepoMock.removeReadingListEntry(user.id, paperId) } returns Unit

        assertDoesNotThrow { mainService.removePaperFromReadingList(paperId.toGrpcId()) }
        coVerify(exactly = 1) { readingListRepoMock.removeReadingListEntry(user.id, paperId) }
    }

    @Test
    fun `When the user removes a non-existent paper on their reading list, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false

        assertThrows<NotFoundException> {
            mainService.removePaperFromReadingList(paperId.toGrpcId())
        }
    }
}
