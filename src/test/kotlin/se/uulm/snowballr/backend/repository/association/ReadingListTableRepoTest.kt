package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.repository.PaperTableRepo
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertUserAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ReadingListTable
import java.util.UUID

class ReadingListTableRepoTest : RepositoryTest(arrayOf(UserTable, PaperTable, ReadingListTable), false) {
    private val repo = ReadingListTableRepo(db)
    private val paperRepo = PaperTableRepo(db)

    @Nested
    inner class CreateReadingListEntry {
        @Test
        fun `When a paper is added to the reading list of a user, then it can be retrieved`() = runTest {
            val paperId = insertPaperAndGetId()
            val userId = insertUserAndGetId("test.user@example.com")
            repo.createReadingListEntry(userId, paperId)
            val actualReadingListEntries = repo.getAllReadingListEntries(userId)
            assertThat(actualReadingListEntries).hasSize(1)
            assertThat(actualReadingListEntries).containsExactly(paperRepo.getPaperById(paperId).getOrThrow())
        }

        @Test
        fun `When a paper is added to the reading list of a user twice, then only one entry is created`() = runTest {
            val paperId = insertPaperAndGetId()
            val userId = insertUserAndGetId("test.user@example.com")
            repo.createReadingListEntry(userId, paperId)
            repo.createReadingListEntry(userId, paperId)
            val actualReadingListEntries = repo.getAllReadingListEntries(userId)
            assertThat(actualReadingListEntries).hasSize(1)
            assertThat(actualReadingListEntries).containsExactly(paperRepo.getPaperById(paperId).getOrThrow())
        }
    }

    @Nested
    inner class RemoveReadingListEntry {
        @Test
        fun `When a paper is removed from the reading list of a user, then it can not be retrieved anymore`() =
            runTest {
                val paperId = insertPaperAndGetId()
                val userId = insertUserAndGetId("test.user@example.com")
                repo.createReadingListEntry(userId, paperId)
                assertThat(repo.getAllReadingListEntries(userId)).hasSize(1)
                repo.removeReadingListEntry(userId, paperId)
                assertThat(repo.getAllReadingListEntries(userId)).hasSize(0)
            }

        @Test
        fun `When a paper that is not on the reading list of a user is removed from it, then no exception is thrown`() =
            runTest {
                val paperId = insertPaperAndGetId()
                val userId = insertUserAndGetId("test.user@example.com")
                assertDoesNotThrow {
                    repo.removeReadingListEntry(userId, paperId)
                }
            }

        @Test
        fun `When a paper is removed from the reading list of a nonexistent user, then no exception is thrown`() =
            runTest {
                val paperId = insertPaperAndGetId()
                assertDoesNotThrow {
                    repo.removeReadingListEntry(UUID.randomUUID(), paperId)
                }
            }

        @Test
        fun `When a nonexistent paper is removed from the reading list of a user, then no exception is thrown`() =
            runTest {
                val userId = insertUserAndGetId("test.user@example.com")
                assertDoesNotThrow {
                    repo.removeReadingListEntry(userId, UUID.randomUUID())
                }
            }
    }

    @Nested
    inner class IsPaperOnReadingList {
        @Test
        fun `When a paper is on the reading list of a user, then isPaperOnReadingList returns true`() = runTest {
            val paperId = insertPaperAndGetId()
            val userId = insertUserAndGetId("test.user@example.com")
            repo.createReadingListEntry(userId, paperId)
            assertTrue(repo.isPaperOnReadingList(userId, paperId))
        }

        @Test
        fun `When a paper is not on the reading list of a user, then isPaperOnReadingList returns false`() = runTest {
            val paperId = insertPaperAndGetId()
            val userId = insertUserAndGetId("test.user@example.com")
            assertFalse(repo.isPaperOnReadingList(userId, paperId))
        }

        @Test
        fun `When a nonexistent paper is provided to isPaperOnReadingList, then it returns false`() = runTest {
            val userId = insertUserAndGetId("test.user@example.com")
            assertFalse(repo.isPaperOnReadingList(userId, UUID.randomUUID()))
        }

        @Test
        fun `When a nonexistent user is provided to isPaperOnReadingList, then it returns false`() = runTest {
            val paperId = insertPaperAndGetId()
            assertFalse(repo.isPaperOnReadingList(UUID.randomUUID(), paperId))
        }
    }

    @Nested
    inner class GetAllReadingListEntries {
        @Test
        fun `When there are no papers on a user's reading list, then getAllReadingListEntries returns an empty list`() =
            runTest {
                val userId = insertUserAndGetId("test.user@example.com")
                assertThat(repo.getAllReadingListEntries(userId)).isEmpty()
            }

        @Test
        fun `When there is one paper on a user's reading list, then getAllReadingListEntries returns a list with that paper`() =
            runTest {
                val paperId = insertPaperAndGetId()
                val userId = insertUserAndGetId("test.user@example.com")
                repo.createReadingListEntry(userId, paperId)

                val actualReadingListEntries = repo.getAllReadingListEntries(userId)
                assertThat(actualReadingListEntries).hasSize(1)
                assertThat(actualReadingListEntries).containsExactly(paperRepo.getPaperById(paperId).getOrThrow())
            }

        @Test
        fun `When there are two papers on a user's reading list, then getAllReadingListEntries returns a list with those two papers`() =
            runTest {
                val paperId1 = insertPaperAndGetId(externalId = "1")
                val paperId2 = insertPaperAndGetId(externalId = "2")
                val userId = insertUserAndGetId("test.user@example.com")
                repo.createReadingListEntry(userId, paperId1)
                repo.createReadingListEntry(userId, paperId2)

                val actualReadingListEntries = repo.getAllReadingListEntries(userId)
                assertThat(actualReadingListEntries).hasSize(2)
                assertThat(actualReadingListEntries).containsExactlyInAnyOrder(
                    paperRepo.getPaperById(paperId1).getOrThrow(),
                    paperRepo.getPaperById(paperId2).getOrThrow(),
                )
            }

        @Test
        fun `When a nonexistent user is provided to getAllReadingListEntries, then it returns an empty list`() =
            runTest {
                assertThat(repo.getAllReadingListEntries(UUID.randomUUID())).isEmpty()
            }
    }
}
