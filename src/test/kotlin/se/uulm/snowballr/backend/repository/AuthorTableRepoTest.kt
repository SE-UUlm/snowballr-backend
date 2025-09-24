package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertAuthorAndGetId
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.util.UUID

class AuthorTableRepoTest : RepositoryTest(arrayOf(AuthorTable), false) {
    private val repo = AuthorTableRepo(db)

    @Nested
    inner class GetAuthorById {
        @Test
        fun `When an author is found, then a successful result with the correct author is returned`() = runTest {
            val authorId = insertAuthorAndGetId("John", "Doe", orcid = "1234")
            val result = repo.getAuthorById(authorId)

            val author = assertResultSuccess(result)
            with(author) {
                assertEquals("John", firstName)
                assertEquals("Doe", lastName)
                assertEquals("1234", orcid)
            }
        }

        @Test
        fun `When an author is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getAuthorById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }
}
