package se.uulm.snowballr.backend.repository

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.table.AuthorTable
import java.util.UUID

class AuthorTableRepoTest : RepositoryTest(arrayOf(AuthorTable), false) {
    private val repo = AuthorTableRepo(db)

    private suspend fun insertTestAuthorAndGetId(
        id: UUID = UUID.randomUUID(),
        firstName: String = "First Name",
        lastName: String = "Last Name",
        orcid: String? = "ORCID",
    ): UUID = db.query {
        AuthorTable.insertAndGetId {
            it[AuthorTable.id] = id
            it[AuthorTable.firstName] = firstName
            it[AuthorTable.lastName] = lastName
            it[AuthorTable.orcid] = orcid
        }.value
    }

    @Nested
    inner class GetAuthorById {
        @Test
        fun `When an author is found, then the correct author is returned`() = runTest {
            val authorId = insertTestAuthorAndGetId()
            val author = repo.getAuthorById(authorId)

            with(author) {
                assertThat(firstName).isEqualTo("First Name")
                assertThat(lastName).isEqualTo("Last Name")
                assertThat(orcid).isEqualTo("ORCID")
            }
        }

        @Test
        fun `When an author is not found, then an exception is thrown`() = runTest {
            assertThrows<NotFoundException> { repo.getAuthorById(UUID.randomUUID()) }
        }
    }
}
