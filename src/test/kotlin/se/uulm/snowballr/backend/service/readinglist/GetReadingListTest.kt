package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.service.MainServiceTest

class GetReadingListTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()
        assertThrows<TestSpecificException> { mainService.getReadingList() }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
        coVerify(exactly = 0) { readingListRepoMock.getAllReadingListEntries(any()) }
    }

    @Test
    fun `When retrieving the reading list entries fails, then an exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getReadingList() }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
        coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
        coVerify(exactly = 0) { authorOfPaperRepoMock.getAuthorsOfPaperById(any()) }
        coVerify(exactly = 0) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(any()) }
    }

    @Test
    fun `When the user has entries on their reading list, then they are correctly returned by getReadingList`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2")
            val author = DataBuilder.createExampleAuthor()

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } returns listOf(paper1, paper2)
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper1.id) } returns emptyList()
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper2.id) } returns listOf(author)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) } returns emptyList()
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns listOf(paper1.id)

            val result = mainService.getReadingList()

            assertThat(result.papersList).containsExactlyInAnyOrder(
                paper1.toGrpcPaper(emptyList(), emptyList()),
                paper2.toGrpcPaper(listOf(author.toGrpcAuthor()), listOf(paper1.id.toString())),
            )

            verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
            coVerify(exactly = 1) { userRepoMock.getUserById(user.id) }
            coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
            coVerify(exactly = 1) { authorOfPaperRepoMock.getAuthorsOfPaperById(paper1.id) }
            coVerify(exactly = 1) { authorOfPaperRepoMock.getAuthorsOfPaperById(paper2.id) }
            coVerify(exactly = 1) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) }
            coVerify(exactly = 1) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) }
        }
}
