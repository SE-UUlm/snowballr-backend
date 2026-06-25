package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.AuthorValidator.validateAuthor
import snowballr.author
import snowballr.PaperOuterClass.Author as GrpcAuthor

class AuthorValidatorTest {
    companion object {
        @JvmStatic
        fun validAuthors() = listOf(
            author { firstName = "John"; lastName = "Doe" },
            author { firstName = ""; lastName = "Doe" },
            author { firstName = "John"; lastName = "" },
        ).map { Arguments.of(it) }

        @JvmStatic
        fun invalidAuthorsTooLongName() = listOf(
            Pair(author { firstName = "a".repeat(AuthorValidator.FIRST_NAME_MAX_LENGTH + 1); lastName = "Doe" }, 1),
            Pair(author { firstName = "John"; lastName = "a".repeat(AuthorValidator.LAST_NAME_MAX_LENGTH + 1) }, 1),
            Pair(
                author {
                    firstName = "a".repeat(AuthorValidator.FIRST_NAME_MAX_LENGTH + 1)
                    lastName = "a".repeat(AuthorValidator.LAST_NAME_MAX_LENGTH + 1)
                },
                2,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.AuthorValidatorTest#validAuthors")
    fun `When an author is valid, then no issues are returned`(author: GrpcAuthor) {
        val result = validateAuthor(author)

        EitherAssert.assertThat(result).isRight()
    }

    @Test
    fun `When an author has a completely blank name, then a 'BlankField' issue is returned`() {
        val author = author {
            firstName = ""
            lastName = ""
        }

        val result = validateAuthor(author)

        assertInvalidResult<BlankField>(result)
        EitherAssert.assertThat(result).isLeft()
        val issues = result.leftOrNull()
        assertNotNull(issues)
        assertThat(issues).hasSize(1)
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.AuthorValidatorTest#invalidAuthorsTooLongName")
    fun `When an author has a too long name, then 'TooLongField' issues are returned`(data: Pair<GrpcAuthor, Int>) {
        val (author, expectedIssues) = data
        val result = validateAuthor(author)

        assertInvalidResult<TooLongField>(result)
        EitherAssert.assertThat(result).isLeft()
        val issues = result.leftOrNull()
        assertNotNull(issues)
        assertThat(issues).hasSize(expectedIssues)
    }
}
