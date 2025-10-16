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
import snowballr.PaperOuterClass.Author as GrpcAuthor

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

    @Nested
    inner class CreateAuthor {
        @Test
        fun `When creating an author, then the returned author contains the correct values`() = runTest {
            val grpcAuthor = GrpcAuthor.newBuilder()
                .setFirstName("John")
                .setLastName("Doe")
                .setOrcid("1234")
                .build()

            val author = repo.createAuthor(grpcAuthor)

            assertEquals("John", author.firstName)
            assertEquals("Doe", author.lastName)
            assertEquals("1234", author.orcid)
        }

        @Test
        fun `When creating an author without ORCID, then the returned author contains null for ORCID`() = runTest {
            val grpcAuthor = GrpcAuthor.newBuilder()
                .setFirstName("Jane")
                .setLastName("Smith")
                .build()

            val author = repo.createAuthor(grpcAuthor)

            assertEquals("Jane", author.firstName)
            assertEquals("Smith", author.lastName)
            assertEquals(null, author.orcid)
        }
    }

    @Nested
    inner class UpdateAuthor {
        @Test
        fun `When updating an existent author, then the returned author contains the updated values`() = runTest {
            val authorId = insertAuthorAndGetId("John", "Doe", orcid = "1234")
            val grpcAuthor = GrpcAuthor.newBuilder()
                .setFirstName("Jane")
                .setLastName("Smith")
                .setOrcid("5678")
                .build()

            val updatedAuthor = repo.updateAuthor(authorId, grpcAuthor)

            assertEquals(authorId, updatedAuthor.id)
            assertEquals("Jane", updatedAuthor.firstName)
            assertEquals("Smith", updatedAuthor.lastName)
            assertEquals("5678", updatedAuthor.orcid)
        }
    }
}
