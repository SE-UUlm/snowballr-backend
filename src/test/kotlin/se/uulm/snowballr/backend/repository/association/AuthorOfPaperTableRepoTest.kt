package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.repository.AuthorTableRepo
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertAuthorAndGetId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.AuthorOfPaperTable

class AuthorOfPaperTableRepoTest : RepositoryTest(arrayOf(AuthorTable, PaperTable, AuthorOfPaperTable), false) {
    private val repo = AuthorOfPaperTableRepo(db)
    private val authorRepo = AuthorTableRepo(db)

    @Nested
    inner class GetAuthorsOfPaperById {
        @Test
        fun `When a paper has no authors, then none are returned`() = runTest {
            val paperId = insertPaperAndGetId()

            assertThat(repo.getAuthorsOfPaperById(paperId)).isEmpty()
        }

        @Test
        fun `When a paper has one author, then it is returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val authorId = insertAuthorAndGetId()

            repo.addAuthorToPaper(authorId, paperId)

            val actualAuthors = repo.getAuthorsOfPaperById(paperId)
            assertThat(actualAuthors).hasSize(1)
            assertThat(actualAuthors).containsExactly(authorRepo.getAuthorById(authorId).getOrThrow())
        }

        @Test
        fun `When a paper has two authors, then they are returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val authorId1 = insertAuthorAndGetId()
            val authorId2 = insertAuthorAndGetId()

            repo.addAuthorToPaper(authorId1, paperId)
            repo.addAuthorToPaper(authorId2, paperId)

            val actualAuthors = repo.getAuthorsOfPaperById(paperId)
            assertThat(actualAuthors).hasSize(2)
            assertThat(actualAuthors).containsExactlyInAnyOrder(
                authorRepo.getAuthorById(authorId1).getOrThrow(),
                authorRepo.getAuthorById(authorId2).getOrThrow(),
            )
        }
    }

    @Nested
    inner class AddAuthorToPaper {
        @Test
        fun `When adding an author to a paper, then the author is added`() = runTest {
            val paperId = insertPaperAndGetId()
            val authorId = insertAuthorAndGetId()

            repo.addAuthorToPaper(authorId, paperId)

            val actualAuthors = repo.getAuthorsOfPaperById(paperId)
            assertThat(actualAuthors).hasSize(1)
            assertThat(actualAuthors).containsExactly(authorRepo.getAuthorById(authorId).getOrThrow())
        }
    }

    @Nested
    inner class RemoveAuthorFromPaper {
        @Test
        fun `When removing an author from a paper, then the author is removed`() = runTest {
            val paperId = insertPaperAndGetId()
            val authorId = insertAuthorAndGetId()

            repo.addAuthorToPaper(authorId, paperId)
            repo.removeAuthorFromPaper(authorId, paperId)

            val actualAuthors = repo.getAuthorsOfPaperById(paperId)
            assertThat(actualAuthors).isEmpty()
        }
    }
}
