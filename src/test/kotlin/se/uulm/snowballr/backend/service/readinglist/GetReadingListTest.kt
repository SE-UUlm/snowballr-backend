package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.service.MainServiceTest

class GetReadingListTest : MainServiceTest() {
    @Test
    fun `When the user has entries on their reading list, then they are correctly returned by getReadingList`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val paper1 = DataBuilder.createExamplePaper()
            val paper2 = DataBuilder.createExamplePaper(externalId = "ExternalId2")

            mockCurrentUser(user)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper2.id) } returns listOf(paper1.id)
            coEvery { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper1.id) } returns emptyList()
            coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } returns listOf(paper1, paper2)

            assertThat(mainService.getReadingList().papersList).containsExactlyInAnyOrder(
                paper1.toGrpcPaper(emptyList()),
                paper2.toGrpcPaper(listOf(paper1.id.toString())),
            )
            coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
        }

    @Test
    fun `When the user has no entries on their reading list, then an empty list is returned`() = runTest {
        val user = DataBuilder.createExampleUser()

        mockCurrentUser(user)
        coEvery { readingListRepoMock.getAllReadingListEntries(user.id) } returns emptyList()

        val papers = mainService.getReadingList().papersList
        assertThat(papers).isEmpty()
        coVerify(exactly = 1) { readingListRepoMock.getAllReadingListEntries(user.id) }
        coVerify(exactly = 0) { citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(any()) }
    }
}
