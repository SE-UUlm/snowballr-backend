package se.uulm.snowballr.backend.repository.association

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.repository.AuthorTableRepo
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.repository.RepositoryTest
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.AuthorOfPaperTable
import java.util.UUID

class AuthorOfPaperTableRepoTest : RepositoryTest(arrayOf(AuthorTable, PaperTable, AuthorOfPaperTable), false) {
    private val repo = AuthorOfPaperTableRepo(db)
    private val authorRepo = AuthorTableRepo(db)

    private suspend fun insertAuthorAndGetId(
        firstName: String = "FirstName",
        lastName: String = "LastName",
        orcid: String? = null,
    ): UUID = db.query {
        AuthorTable.insertAndGetId {
            it[AuthorTable.firstName] = firstName
            it[AuthorTable.lastName] = lastName
            it[AuthorTable.orcid] = orcid
        }.value
    }

    private suspend fun addAuthorToPaper(authorId: UUID, paperId: UUID) = db.query {
        AuthorOfPaperTable.insertAndGetId {
            it[AuthorOfPaperTable.paperId] = paperId
            it[AuthorOfPaperTable.authorId] = authorId
        }
    }

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
            addAuthorToPaper(authorId, paperId)
            val actualAuthors = repo.getAuthorsOfPaperById(paperId)
            assertThat(actualAuthors).hasSize(1)
            assertThat(actualAuthors).containsExactly(authorRepo.getAuthorById(authorId))
        }

        @Test
        fun `When a paper has two authors, then they are returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val authorId1 = insertAuthorAndGetId()
            val authorId2 = insertAuthorAndGetId()
            addAuthorToPaper(authorId1, paperId)
            addAuthorToPaper(authorId2, paperId)
            val actualAuthors = repo.getAuthorsOfPaperById(paperId)
            assertThat(actualAuthors).hasSize(2)
            assertThat(actualAuthors).containsExactlyInAnyOrder(
                authorRepo.getAuthorById(authorId1),
                authorRepo.getAuthorById(authorId2),
            )
        }
    }
}
