package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingListIntegrationTest : IntegrationTest() {
    @Nested
    inner class AddPaperToReadingList {
        @Test
        fun `When a paper is added to the reading list, then it appears in the reading list`() = runTest {
            val paper = createPaper()

            readingListService.addPaperToReadingList(paper.id)

            val readingList = readingListService.getReadingList()
            assertTrue(readingList.any { it.id == paper.id })
        }

        @Test
        fun `When multiple papers are added to the reading list, then all appear in the reading list`() = runTest {
            val paperOne = createPaper("Paper One")
            val paperTwo = createPaper("Paper Two")

            readingListService.addPaperToReadingList(paperOne.id)
            readingListService.addPaperToReadingList(paperTwo.id)

            val readingList = readingListService.getReadingList()
            val ids = readingList.map { it.id }
            assertTrue(ids.contains(paperOne.id))
            assertTrue(ids.contains(paperTwo.id))
        }

        @Test
        fun `When a paper is added to the reading list, then isPaperOnReadingList returns true`() = runTest {
            val paper = createPaper()

            readingListService.addPaperToReadingList(paper.id)

            assertTrue(readingListService.isPaperOnReadingList(paper.id))
        }
    }

    @Nested
    inner class RemovePaperFromReadingList {
        @Test
        fun `When a paper is removed from the reading list, then it no longer appears in the reading list`() = runTest {
            val paper = createPaper()

            readingListService.addPaperToReadingList(paper.id)
            readingListService.removePaperFromReadingList(paper.id)

            val readingList = readingListService.getReadingList()
            assertFalse(readingList.any { it.id == paper.id })
        }

        @Test
        fun `When a paper is removed from the reading list, then isPaperOnReadingList returns false`() = runTest {
            val paper = createPaper()

            readingListService.addPaperToReadingList(paper.id)
            readingListService.removePaperFromReadingList(paper.id)

            assertFalse(readingListService.isPaperOnReadingList(paper.id))
        }

        @Test
        fun `When one of two papers is removed from the reading list, then the other paper remains`() = runTest {
            val paperOne = createPaper("Paper One")
            val paperTwo = createPaper("Paper Two")

            readingListService.addPaperToReadingList(paperOne.id)
            readingListService.addPaperToReadingList(paperTwo.id)
            readingListService.removePaperFromReadingList(paperOne.id)

            val readingList = readingListService.getReadingList()
            assertFalse(readingList.any { it.id == paperOne.id })
            assertTrue(readingList.any { it.id == paperTwo.id })
        }
    }

    @Nested
    inner class GetReadingList {
        @Test
        fun `When no papers have been added, then the reading list is empty`() = runTest {
            val readingList = readingListService.getReadingList()

            assertTrue(readingList.isEmpty())
        }

        @Test
        fun `When a paper is added to one user's reading list, then it does not appear in another user's reading list`() =
            runTest {
                val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
                val paper = createPaper()

                readingListService.addPaperToReadingList(paper.id)

                actAsUser(otherUser.id) {
                    val otherReadingList = readingListService.getReadingList()
                    assertFalse(otherReadingList.any { it.id == paper.id })
                }
            }
    }

    @Nested
    inner class IsPaperOnReadingList {
        @Test
        fun `When a paper has not been added to the reading list, then isPaperOnReadingList returns false`() = runTest {
            val paper = createPaper()

            assertFalse(readingListService.isPaperOnReadingList(paper.id))
        }
    }
}
