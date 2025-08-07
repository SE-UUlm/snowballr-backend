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

class AddPaperToReadingListTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        val paperId = Base.Id.getDefaultInstance()
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
        assertThrows<TestSpecificException> { mainService.addPaperToReadingList(paperId) }
    }

    @Test
    fun `When creating the reading list entry fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { readingListRepoMock.createReadingListEntry(user.id, paperId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.addPaperToReadingList(paperId.toGrpcId()) }
    }

    @Test
    fun `When the user adds a paper to their reading list, then the request is forwarded correctly`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns true
        coEvery { readingListRepoMock.createReadingListEntry(user.id, paperId) } returns Unit
        coEvery { userRepoMock.getUserById(user.id) } returns user

        assertDoesNotThrow { mainService.addPaperToReadingList(paperId.toGrpcId()) }
        coVerify(exactly = 1) { readingListRepoMock.createReadingListEntry(user.id, paperId) }
    }

    @Test
    fun `When the user adds a non-existent paper to their reading list, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val paperId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { paperRepoMock.doesPaperExistById(paperId) } returns false
        coEvery { userRepoMock.getUserById(user.id) } returns user

        assertThrows<NotFoundException> { mainService.addPaperToReadingList(paperId.toGrpcId()) }
    }
}
