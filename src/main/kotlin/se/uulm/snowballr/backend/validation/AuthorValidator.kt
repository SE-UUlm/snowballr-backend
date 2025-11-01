package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.PaperOuterClass.Author

object AuthorValidator {
    const val FIRST_NAME_MAX_LENGTH = 100
    const val LAST_NAME_MAX_LENGTH = 100

    fun validateAuthor(author: Author): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureTextFieldValidity("first_name", author.firstName, FIRST_NAME_MAX_LENGTH) },
            { ensureTextFieldValidity("last_name", author.lastName, LAST_NAME_MAX_LENGTH) },
        ) { _, _ -> }
    }
}
