package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.parseUUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingListIntegrationTest : IntegrationTest() {
    @Nested
    inner class AddPaperToReadingList {
        @Test
        fun `When a paper is added to the reading list, then it appears in the reading list`() = runTest {
            val paper = createPaper()
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            readingListService.addPaperToReadingList(paperId)

            val readingList = readingListService.getReadingList()
            assertTrue(readingList.papersList.any { it.id == paper.id })
        }

        @Test
        fun `When multiple papers are added to the reading list, then all appear in the reading list`() = runTest {
            val paperOne = createPaper("Paper One")
            val paperTwo = createPaper("Paper Two")
            val paperOneId = parseUUID(paperOne.id, EntityType.PAPER)
            val paperTwoId = parseUUID(paperTwo.id, EntityType.PAPER)

            readingListService.addPaperToReadingList(paperOneId)
            readingListService.addPaperToReadingList(paperTwoId)

            val readingList = readingListService.getReadingList()
            val ids = readingList.papersList.map { it.id }
            assertTrue(ids.contains(paperOne.id))
            assertTrue(ids.contains(paperTwo.id))
        }

        @Test
        fun `When a paper is added to the reading list, then isPaperOnReadingList returns true`() = runTest {
            val paper = createPaper()
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            readingListService.addPaperToReadingList(paperId)

            assertTrue(readingListService.isPaperOnReadingList(paperId))
        }
    }

    @Nested
    inner class RemovePaperFromReadingList {
        @Test
        fun `When a paper is removed from the reading list, then it no longer appears in the reading list`() = runTest {
            val paper = createPaper()
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            readingListService.addPaperToReadingList(paperId)
            readingListService.removePaperFromReadingList(paperId)

            val readingList = readingListService.getReadingList()
            assertFalse(readingList.papersList.any { it.id == paper.id })
        }

        @Test
        fun `When a paper is removed from the reading list, then isPaperOnReadingList returns false`() = runTest {
            val paper = createPaper()
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            readingListService.addPaperToReadingList(paperId)
            readingListService.removePaperFromReadingList(paperId)

            assertFalse(readingListService.isPaperOnReadingList(paperId))
        }

        @Test
        fun `When one of two papers is removed from the reading list, then the other paper remains`() = runTest {
            val paperOne = createPaper("Paper One")
            val paperTwo = createPaper("Paper Two")
            val paperOneId = parseUUID(paperOne.id, EntityType.PAPER)
            val paperTwoId = parseUUID(paperTwo.id, EntityType.PAPER)

            readingListService.addPaperToReadingList(paperOneId)
            readingListService.addPaperToReadingList(paperTwoId)
            readingListService.removePaperFromReadingList(paperOneId)

            val readingList = readingListService.getReadingList()
            assertFalse(readingList.papersList.any { it.id == paperOne.id })
            assertTrue(readingList.papersList.any { it.id == paperTwo.id })
        }
    }

    @Nested
    inner class GetReadingList {
        @Test
        fun `When no papers have been added, then the reading list is empty`() = runTest {
            val readingList = readingListService.getReadingList()

            assertTrue(readingList.papersList.isEmpty())
        }

        @Test
        fun `When a paper is added to one user's reading list, then it does not appear in another user's reading list`() =
            runTest {
                val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
                val otherUserId = parseUUID(otherUser.id, EntityType.USER)
                val paper = createPaper()
                val paperId = parseUUID(paper.id, EntityType.PAPER)

                readingListService.addPaperToReadingList(paperId)

                actAsUser(otherUserId) {
                    val otherReadingList = readingListService.getReadingList()
                    assertFalse(otherReadingList.papersList.any { it.id == paper.id })
                }
            }
    }

    @Nested
    inner class IsPaperOnReadingList {
        @Test
        fun `When a paper has not been added to the reading list, then isPaperOnReadingList returns false`() = runTest {
            val paper = createPaper()
            val paperId = parseUUID(paper.id, EntityType.PAPER)

            assertFalse(readingListService.isPaperOnReadingList(paperId))
        }
    }
}
