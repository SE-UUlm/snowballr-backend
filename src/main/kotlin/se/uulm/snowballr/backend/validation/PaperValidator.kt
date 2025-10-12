package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.TooLongList
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.PaperOuterClass.Paper
import java.time.LocalDate

object PaperValidator {
    const val EXTERNAL_ID_MAX_LENGTH = 100
    const val TITLE_MAX_LENGTH = 100
    const val ABSTRACT_MAX_LENGTH = 3000
    const val YEAR_MIN_VALUE = 0
    const val PUBLISHER_MAX_LENGTH = 100
    const val PUBLICATION_NAME_MAX_LENGTH = 100
    const val PUBLICATION_TYPE_MAX_LENGTH = 100
    const val MAX_AUTHOR_COUNT = 500

    fun validateUpdateRequest(request: Paper.Update): EitherNel<ValidationIssue, Unit> = either {
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, Paper.Update.getDescriptor(), listOf("paper.has_pdf"))
        }

        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        val selectedFields = request.mask.pathsList.toSet()

        val paper = request.paper

        validatePaperProps(paper, selectedFields)
        validateAuthors(paper, selectedFields)
    }

    @Suppress("CognitiveComplexMethod", "kotlin:S3776")
    private fun Raise<Nel<ValidationIssue>>.validatePaperProps(paper: Paper, selectedFields: Set<String>) {
        @Suppress("NamedArguments")
        zipOrAccumulate(
            { ensureIdValidity("id", paper.id) },
            {
                if ("paper.external_id" in selectedFields) {
                    ensureFieldLength("external_id", paper.externalId, EXTERNAL_ID_MAX_LENGTH)
                }
            },
            {
                if ("paper.title" in selectedFields) {
                    ensureTextFieldValidity("title", paper.title, TITLE_MAX_LENGTH)
                }
            },
            {
                if ("paper.abstrakt" in selectedFields) {
                    ensureFieldLength("abstrakt", paper.abstrakt, ABSTRACT_MAX_LENGTH)
                }
            },
            {
                if ("paper.year" in selectedFields) {
                    val nextYear = LocalDate.now().year + 1
                    ensureNumberFieldInRange("year", paper.year, YEAR_MIN_VALUE, nextYear)
                }
            },
            {
                if ("paper.publisher" in selectedFields) {
                    ensureFieldLength("publisher", paper.publisher, PUBLISHER_MAX_LENGTH)
                }
            },
            {
                if ("paper.publication_name" in selectedFields) {
                    ensureFieldLength("publication_name", paper.publicationName, PUBLICATION_NAME_MAX_LENGTH)
                }
            },
            {
                if ("paper.publication_type" in selectedFields) {
                    ensureFieldLength("publication_type", paper.publicationType, PUBLICATION_TYPE_MAX_LENGTH)
                }
            },
            {
                if ("paper.authors" in selectedFields) {
                    ensure(paper.authorsCount <= MAX_AUTHOR_COUNT) {
                        TooLongList("authors", MAX_AUTHOR_COUNT)
                    }
                }
            },
        ) { _, _, _, _, _, _, _, _, _ -> }
    }

    private fun Raise<Nel<ValidationIssue>>.validateAuthors(paper: Paper, selectedFields: Set<String>) {
        if ("paper.authors" !in selectedFields) return

        val validations = paper.authorsList.map(AuthorValidator::validateAuthor)
        val issues = validations.filterIsInstance<Either.Left<Nel<ValidationIssue>>>().map { it.value }
        issues.reduceOrNull { acc, nel -> acc + nel }?.let { raise(it) }
    }
}
