package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse

class GetReadingListTest : ReadingListServiceTest() {
    @Test
    fun `When the user has entries on their reading list, then they are correctly returned by getReadingList`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paper1 = DataBuilder.createExamplePaper()
            val author = DataBuilder.createExampleAuthor()
            val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2", authors = listOf(author))

            mockCurrentUser(user)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns listOf(paper1.id)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) } returns emptyList()
            coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } returns listOf(paper1, paper2)

            assertThat(service.getReadingList()).containsExactlyInAnyOrder(
                PaperResponse.fromPaper(paper1, emptyList()),
                PaperResponse.fromPaper(paper2, listOf(paper1.id)),
            )
            coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
        }

    @Test
    fun `When the user has no entries on their reading list, then an empty list is returned`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)
        coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } returns emptyList()

        val papers = service.getReadingList()

        assertThat(papers).isEmpty()
        coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
        coVerify(exactly = 0) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(any()) }
    }
}
