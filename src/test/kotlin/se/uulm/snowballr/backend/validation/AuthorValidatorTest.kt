package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import se.uulm.snowballr.backend.DataBuilder.createValidOrcid
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidOrcid
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.AuthorValidator.validateAuthor
import snowballr.author
import snowballr.copy

class AuthorValidatorTest {
    private val validAuthor = author {
        firstName = "John"
        lastName = "Doe"
        orcid = createValidOrcid()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "0000-0002-9079-593X",
            "0000-0001-7195-7801",
            "0000-0002-9067-3748",
            "0000-0002-6088-8393",
        ],
    )
    fun `When an author is valid, then no issues are returned`(validOrcid: String) {
        val author = validAuthor.copy {
            orcid = validOrcid
        }

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

    @Test
    fun `When an author has an empty orcid, then no issue is returned`() {
        val author = validAuthor.copy {
            orcid = ""
        }

        val result = validateAuthor(author)

        EitherAssert.assertThat(result).isRight()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "1234",
            "1234-5678-9012-3456-7890",
            "1234-5678-90ab-cdef",
            "1234-5678-9012-345x",
            "12345-67890-12345-x",
        ],
    )
    fun `When an author has an invalid orcid format, then an 'InvalidOrcid' issue is returned`(invalidOrcid: String) {
        val author = validAuthor.copy {
            orcid = invalidOrcid
        }

        val result = validateAuthor(author)

        assertInvalidResult<InvalidOrcid>(result)
    }

    @Test
    fun `When an author has an invalid orcid check digit, then an 'InvalidOrcid' is returned`() {
        var invalidOrcid = createValidOrcid()
        invalidOrcid = ((invalidOrcid[0].digitToInt() + 1) % 10).toString() + invalidOrcid.substring(1)
        val author = validAuthor.copy {
            orcid = invalidOrcid
        }

        val result = validateAuthor(author)

        assertInvalidResult<InvalidOrcid>(result)
    }
}
