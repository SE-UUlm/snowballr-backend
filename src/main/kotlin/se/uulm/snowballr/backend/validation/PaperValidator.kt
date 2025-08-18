package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.TooLongList
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.PaperOuterClass.Paper

object PaperValidator {
    private const val EXTERNAL_ID_MAX_LENGTH = 100
    private const val TITLE_MAX_LENGTH = 100
    private const val ABSTRACT_MAX_LENGTH = 3000
    private const val YEAR_MIN_VALUE = 0
    private const val PUBLISHER_MAX_LENGTH = 100
    private const val PUBLICATION_NAME_MAX_LENGTH = 100
    private const val PUBLICATION_TYPE_MAX_LENGTH = 100
    private const val MAX_AUTHOR_COUNT = 500

    @Suppress("CognitiveComplexMethod", "kotlin:S3776")
    fun validateUpdateRequest(request: Paper.Update): EitherNel<ValidationIssue, Unit> = either {
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, Paper.Update.getDescriptor())
        }

        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        val selectedFields = request.mask.pathsList.toSet()

        val paper = request.paper
        @Suppress("NamedArguments")
        zipOrAccumulate(
            { ensureIdValidity("id", paper.id) },
            {
                if ("paper.external_id" in selectedFields) {
                    ensureTextFieldValidity("external_id", paper.externalId, EXTERNAL_ID_MAX_LENGTH)
                }
            },
            {
                if ("paper.title" in selectedFields) {
                    ensureTextFieldValidity("title", paper.title, TITLE_MAX_LENGTH)
                }
            },
            {
                if ("paper.abstrakt" in selectedFields) {
                    ensureTextFieldValidity("abstrakt", paper.abstrakt, ABSTRACT_MAX_LENGTH)
                }
            },
            {
                if ("paper.year" in selectedFields) {
                    val currentYear = java.time.LocalDate.now().year
                    ensureNumberFieldInRange("year", paper.year, YEAR_MIN_VALUE, currentYear)
                }
            },
            {
                if ("paper.publisher" in selectedFields) {
                    ensureTextFieldValidity("publisher", paper.publisher, PUBLISHER_MAX_LENGTH)
                }
            },
            {
                if ("paper.publication_name" in selectedFields) {
                    ensureTextFieldValidity("publication_name", paper.publicationName, PUBLICATION_NAME_MAX_LENGTH)
                }
            },
            {
                if ("paper.publication_type" in selectedFields) {
                    ensureTextFieldValidity("publication_type", paper.publicationType, PUBLICATION_TYPE_MAX_LENGTH)
                }
            },
            {
                if ("paper.authors" in selectedFields) {
                    ensure(request.paper.authorsCount <= MAX_AUTHOR_COUNT) {
                        TooLongList("authors", MAX_AUTHOR_COUNT)
                    }
                }
            },
        ) { _, _, _, _, _, _, _, _, _ -> }
    }
}
