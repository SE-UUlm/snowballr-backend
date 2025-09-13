package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.toGrpcAuthor
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.service.MainServiceTest

class GetReadingListTest : MainServiceTest() {
    @Test
    fun `When retrieving the reading list entries fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)
        coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getReadingList() }
    }

    @Test
    fun `When the user has entries on their reading list, then they are correctly returned by getReadingList`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2")
            val author = DataBuilder.createExampleAuthor()

            mockCurrentUser(user)
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper1.id) } returns emptyList()
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper2.id) } returns listOf(author)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns listOf(paper1.id)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) } returns emptyList()
            coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } returns listOf(paper1, paper2)

            assertThat(mainService.getReadingList().papersList).containsExactlyInAnyOrder(
                paper1.toGrpcPaper(emptyList(), emptyList()),
                paper2.toGrpcPaper(listOf(author.toGrpcAuthor()), listOf(paper1.id.toString())),
            )
            coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
        }
}
