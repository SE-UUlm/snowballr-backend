package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.PaperOuterClass.Author

object AuthorValidator {
    const val FIRST_NAME_MAX_LENGTH = 100
    const val LAST_NAME_MAX_LENGTH = 100

    fun validateAuthor(author: Author): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureFieldLength("first_name", author.firstName, FIRST_NAME_MAX_LENGTH) },
            { ensureFieldLength("last_name", author.lastName, LAST_NAME_MAX_LENGTH) },
            { ensureOnlyOneBlankField(author) },
        ) { _, _, _ -> }
    }

    /**
     * Ensures that the given author has at least one non-blank name field.
     *
     * @param author The author to validate.
     */
    private fun Raise<ValidationIssue>.ensureOnlyOneBlankField(author: Author) {
        ensure(
            author.firstName.isNotBlank() || author.lastName.isNotBlank(),
        ) { BlankField("first_name' and 'last_name") }
    }
}
