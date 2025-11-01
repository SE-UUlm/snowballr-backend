package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.AuthorValidator.validateAuthor
import snowballr.author
import snowballr.copy

class AuthorValidatorTest {
    private val validAuthor = author {
        firstName = "John"
        lastName = "Doe"
    }

    @Test
    fun `When an author is valid, then no issues are returned`() {
        val author = validAuthor

        val result = validateAuthor(author)

        EitherAssert.assertThat(result).isRight()
    }

    @Test
    fun `When an author has a blank name, then 'BlankField' issues are returned`() {
        val author = validAuthor.copy {
            firstName = ""
            lastName = ""
        }

        val result = validateAuthor(author)

        assertInvalidResult<BlankField>(result)
        EitherAssert.assertThat(result).isLeft()
        val issues = result.leftOrNull()
        assertNotNull(issues)
        assertThat(issues).hasSize(2)
    }

    @Test
    fun `When an author has a too long name, then 'TooLongField' issues are returned`() {
        val author = validAuthor.copy {
            firstName = "a".repeat(AuthorValidator.FIRST_NAME_MAX_LENGTH + 1)
            lastName = "a".repeat(AuthorValidator.LAST_NAME_MAX_LENGTH + 1)
        }

        val result = validateAuthor(author)

        assertInvalidResult<TooLongField>(result)
        EitherAssert.assertThat(result).isLeft()
        val issues = result.leftOrNull()
        assertNotNull(issues)
        assertThat(issues).hasSize(2)
    }
}
