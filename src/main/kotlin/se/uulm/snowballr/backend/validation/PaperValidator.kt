package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.model.CompositeIssue
import se.uulm.snowballr.backend.model.MultipleOccurrences
import se.uulm.snowballr.backend.model.TooLongList
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.model.dto.paper.PaperField
import snowballr.PaperOuterClass.Paper
import java.time.LocalDate

@Suppress("TooManyFunctions")
object PaperValidator {
    const val TITLE_MAX_LENGTH = 250
    const val ABSTRACT_MAX_LENGTH = 3500
    const val YEAR_MIN_VALUE = 0
    const val PUBLISHER_MAX_LENGTH = 200
    const val PUBLICATION_NAME_MAX_LENGTH = 200
    const val PUBLICATION_TYPE_MAX_LENGTH = 200
    const val MAX_AUTHOR_COUNT = 500

    private const val FIELD_PAPER_ID = "paper.id"
    private const val FIELD_PAPER_AUTHORS = "paper.authors"
    private const val FIELD_PAPER_PUBLICATION_TYPE = "paper.publication_type"
    private const val FIELD_PAPER_PUBLICATION_NAME = "paper.publication_name"
    private const val FIELD_PAPER_PUBLISHER = "paper.publisher"
    private const val FIELD_PAPER_YEAR = "paper.year"
    private const val FIELD_PAPER_ABSTRACT = "paper.abstrakt"
    private const val FIELD_PAPER_TITLE = "paper.title"
    private const val FIELD_PAPER_EXTERNAL_IDS = "paper.external_ids"

    fun validateCreateRequest(request: Paper): EitherNel<ValidationIssue, Unit> = either {
        validatePaperProps(request, ignoreId = true)
        validateAuthors(request)
        validateExternalIds(request)
    }

    fun validateUpdateRequest(request: Paper.Update): EitherNel<ValidationIssue, Unit> = either {
        validateUpdateFieldMask(request).bind()
        val selectedFields = FieldMaskUtil.normalize(request.mask).pathsList.toSet()

        val paper = request.paper
        validatePaperProps(paper, selectedFields)
        validateAuthors(paper, selectedFields)
        validateExternalIds(paper, selectedFields)
    }

    fun getGrpcPathsForPaperField(field: PaperField) = when (field) {
        PaperField.TITLE -> "paper.title"
        PaperField.ABSTRACT -> "paper.abstrakt"
        PaperField.YEAR -> "paper.year"
        PaperField.PUBLISHER -> "paper.publisher"
        PaperField.PUBLICATION_NAME -> "paper.publication_name"
        PaperField.PUBLICATION_TYPE -> "paper.publication_type"
        PaperField.AUTHORS -> "paper.authors"
        PaperField.EXTERNAL_IDS -> "paper.external_ids"
    }

    private fun validateUpdateFieldMask(request: Paper.Update): EitherNel<ValidationIssue, Unit> = either {
        val allowedPaths = listOf(FIELD_PAPER_ID) + PaperField.entries.map { getGrpcPathsForPaperField(it) }
        ensureFieldMaskIsValid(request.mask, allowedPaths)
    }.toEitherNel()

    private fun Raise<Nel<ValidationIssue>>.validatePaperProps(
        paper: Paper,
        selectedFields: Set<String> = emptySet(),
        ignoreId: Boolean = false,
    ) {
        val has = { path: String -> hasPathOrIsEmpty(selectedFields, path) }

        @Suppress("NamedArguments")
        zipOrAccumulate(
            { validatePaperId(ignoreId, paper) },
            { validateExternalIdsList(has, paper) },
            { validateTitle(has, paper) },
            { validateAbstract(has, paper) },
            { validateYear(has, paper) },
            { validatePublisher(has, paper) },
            { validatePublicationName(has, paper) },
            { validatePublicationType(has, paper) },
            { validateAuthorList(has, paper) },
        ) { _, _, _, _, _, _, _, _, _ -> }
    }

    private fun RaiseAccumulate<ValidationIssue>.validateAuthorList(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_AUTHORS)) {
            ensure(paper.authorsCount <= MAX_AUTHOR_COUNT) {
                TooLongList(FIELD_PAPER_AUTHORS, MAX_AUTHOR_COUNT)
            }
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validateExternalIdsList(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_EXTERNAL_IDS)) {
            val multipleOccurrences = paper.externalIdsList.groupBy { it.type }
                .map { it.key to it.value.size }
                .filter { it.second > 1 }

            ensure(multipleOccurrences.isEmpty()) {
                MultipleOccurrences("external IDs", multipleOccurrences)
            }
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validatePublicationType(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_PUBLICATION_TYPE)) {
            ensureFieldLength(FIELD_PAPER_PUBLICATION_TYPE, paper.publicationType, PUBLICATION_TYPE_MAX_LENGTH)
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validatePublicationName(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_PUBLICATION_NAME)) {
            ensureFieldLength(FIELD_PAPER_PUBLICATION_NAME, paper.publicationName, PUBLICATION_NAME_MAX_LENGTH)
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validatePublisher(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_PUBLISHER)) {
            ensureFieldLength(FIELD_PAPER_PUBLISHER, paper.publisher, PUBLISHER_MAX_LENGTH)
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validateYear(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_YEAR)) {
            val nextYear = LocalDate.now().year + 1
            ensureNumberFieldInRange(FIELD_PAPER_YEAR, paper.year, YEAR_MIN_VALUE, nextYear)
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validateAbstract(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_ABSTRACT)) {
            ensureFieldLength(FIELD_PAPER_ABSTRACT, paper.abstrakt, ABSTRACT_MAX_LENGTH)
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validateTitle(has: (String) -> Boolean, paper: Paper) {
        if (has(FIELD_PAPER_TITLE)) {
            ensureTextFieldValidity(FIELD_PAPER_TITLE, paper.title, TITLE_MAX_LENGTH)
        }
    }

    private fun RaiseAccumulate<ValidationIssue>.validatePaperId(ignoreId: Boolean, paper: Paper) {
        if (!ignoreId) {
            ensureIdValidity("id", paper.id)
        }
    }

    private fun Raise<Nel<ValidationIssue>>.validateAuthors(paper: Paper, selectedFields: Set<String> = emptySet()) =
        validateElementList(
            FIELD_PAPER_AUTHORS,
            paper.authorsList,
            AuthorValidator::validateAuthor,
            "author",
            selectedFields,
        )

    private fun Raise<Nel<ValidationIssue>>.validateExternalIds(
        paper: Paper,
        selectedFields: Set<String> = emptySet(),
    ) = validateElementList(
        FIELD_PAPER_EXTERNAL_IDS,
        paper.externalIdsList,
        ExternalIdValidator::validateExternalId,
        "external ID",
        selectedFields,
    )

    private fun <T> Raise<Nel<ValidationIssue>>.validateElementList(
        fieldPath: String,
        elements: List<T>,
        validateFn: (T) -> EitherNel<ValidationIssue, Unit>,
        elementName: String,
        selectedFields: Set<String>,
    ) {
        if (!hasPathOrIsEmpty(selectedFields, fieldPath)) return

        val validations = elements.mapIndexed { i, element ->
            val result = validateFn(element)
            if (result is Either.Left) {
                val issues = result.value.toList()
                val compositeIssue = CompositeIssue("Issues of $elementName at index $i", issues)
                Either.Left(nonEmptyListOf(compositeIssue))
            } else {
                result
            }
        }
        val issues = validations.filterIsInstance<Either.Left<Nel<ValidationIssue>>>().map { it.value }
        issues.reduceOrNull { acc, nel -> acc + nel }?.let { raise(it) }
    }

    /**
     * Checks whether the given path is included in the selected fields or if no fields are selected.
     *
     * Either use this with a set of selected fields from a field mask, or an empty set to indicate that all fields are
     * selected, i.e., no field mask was provided.
     *
     * @param selectedFields The set of selected field paths.
     * @param path The specific field path to check.
     * @return `true` if the selected fields are empty or if the path is included in the selected fields; `false`
     * otherwise.
     */
    private fun hasPathOrIsEmpty(selectedFields: Set<String>, path: String) =
        selectedFields.isEmpty() || path in selectedFields
}
