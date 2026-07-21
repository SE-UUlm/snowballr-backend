package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.CompositeIssue
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.OutOfRangeValue
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.model.TooLongList
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.validation.PaperValidator.ABSTRACT_MAX_LENGTH
import se.uulm.snowballr.backend.validation.PaperValidator.EXTERNAL_ID_MAX_LENGTH
import se.uulm.snowballr.backend.validation.PaperValidator.MAX_AUTHOR_COUNT
import se.uulm.snowballr.backend.validation.PaperValidator.PUBLICATION_NAME_MAX_LENGTH
import se.uulm.snowballr.backend.validation.PaperValidator.PUBLICATION_TYPE_MAX_LENGTH
import se.uulm.snowballr.backend.validation.PaperValidator.PUBLISHER_MAX_LENGTH
import se.uulm.snowballr.backend.validation.PaperValidator.TITLE_MAX_LENGTH
import se.uulm.snowballr.backend.validation.PaperValidator.YEAR_MIN_VALUE
import snowballr.PaperOuterClass
import snowballr.PaperOuterClass.Paper
import snowballr.author
import java.time.LocalDate
import java.util.UUID

class PaperValidatorTest {
    companion object {
        private val blankableTextFields =
            listOf(
                Pair("external_id", EXTERNAL_ID_MAX_LENGTH),
                Pair("abstrakt", ABSTRACT_MAX_LENGTH),
                Pair("publisher", PUBLISHER_MAX_LENGTH),
                Pair("publication_name", PUBLICATION_NAME_MAX_LENGTH),
                Pair("publication_type", PUBLICATION_TYPE_MAX_LENGTH),
            )

        private val nonBlankableTextFields =
            listOf(
                Pair("title", TITLE_MAX_LENGTH),
            )

        private val allTextFields = blankableTextFields + nonBlankableTextFields

        @JvmStatic
        fun invalidFields(): List<Arguments> = listOf(
            Arguments.of(emptyList<String>(), "no paths"),
            Arguments.of(listOf("non_existent_field"), "a non-existent field"),
            Arguments.of(listOf("has_pdf"), "a field that is not allowed in the mask"),
        )

        @JvmStatic
        fun blankFieldTestProvider(): List<Arguments> = nonBlankableTextFields.map { (fieldName) ->
            Arguments.of(
                fieldName,
                "When a blank '$fieldName' field is validated, then the 'BlankField' issue is returned",
            )
        }

        @JvmStatic
        fun blankFieldTestProviderWithFieldMask(): List<Arguments> {
            val testName: (Any?, Boolean) -> String = { fieldName, isFieldInMask ->
                requireNotNull(fieldName) { "fieldName must not be null" }

                if (isFieldInMask) {
                    "When a blank '$fieldName' field is validated and specified in the field mask, then the " +
                        "'BlankField' issue is returned"
                } else {
                    "When a blank '$fieldName' field is validated but not specified in the field mask, then " +
                        "no issue is returned"
                }
            }

            return blankFieldTestProvider()
                .map { args -> args.get() }
                .flatMap { args ->
                    listOf(
                        Arguments.of(args[0], false, testName(args[0], false)),
                        Arguments.of(args[0], true, testName(args[0], true)),
                    )
                }
        }

        @JvmStatic
        fun tooLongFieldTestProvider(): List<Arguments> = allTextFields.map { (fieldName, maxLength) ->
            Arguments.of(
                fieldName,
                maxLength,
                "When a too long '$fieldName' field is validated ($maxLength chars), then the 'TooLongField' " +
                    "issue is returned",
            )
        }

        @JvmStatic
        fun tooLongFieldTestProviderWithFieldMask(): List<Arguments> {
            val testName: (Any?, Any?, Boolean) -> String = { fieldName, maxLength, isFieldInMask ->
                requireNotNull(fieldName) { "fieldName must not be null" }
                requireNotNull(maxLength) { "maxLength must not be null" }

                if (isFieldInMask) {
                    "When a too long field '$fieldName' ($maxLength chars) is validated and specified in the " +
                        "field mask, then the 'TooLongField' issue is returned"
                } else {
                    "When a too long field '$fieldName' ($maxLength chars) is validated but not specified in the " +
                        "field mask, then no issue is returned"
                }
            }

            return tooLongFieldTestProvider()
                .map { args -> args.get() }
                .flatMap { args ->
                    listOf(
                        Arguments.of(args[0], args[1], false, testName(args[0], args[1], false)),
                        Arguments.of(args[0], args[1], true, testName(args[0], args[1], true)),
                    )
                }
        }
    }

    private val validPaperBuilder: Paper.Builder = Paper.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setExternalId("some-doi")
        .setTitle("This is a paper title")
        .setAbstrakt("This is the abstract of a paper")
        .setYear(LocalDate.now().year)
        .setPublisher("IEEE")
        .setPublicationName("IEEE Journal")
        .setPublicationType("journal")
        .addAllAuthors(
            (1..5).map {
                author {
                    firstName = "John$it"
                    lastName = "Doe$it"
                }
            },
        )

    @Nested
    inner class CreateRequest {
        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validPaperBuilder
                .setId("   ") // ID is ignored during creation, so it can be blank
                .build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("se.uulm.snowballr.backend.validation.PaperValidatorTest#blankFieldTestProvider")
        fun `When a blank field is validated, then expected result is returned`(
            fieldName: String,
            @Suppress("unused") testName: String,
        ) {
            val fieldDescriptor = Paper.getDescriptor().findFieldByName(fieldName)
            val request = validPaperBuilder.setField(fieldDescriptor, " ").build()

            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("se.uulm.snowballr.backend.validation.PaperValidatorTest#tooLongFieldTestProvider")
        fun `When a too long field is validated, then expected result is returned`(
            fieldName: String,
            maxLength: Int,
            @Suppress("unused") testName: String,
        ) {
            val fieldDescriptor = Paper.getDescriptor().findFieldByName(fieldName)
            val request = validPaperBuilder.setField(fieldDescriptor, "a".repeat(maxLength + 1)).build()

            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }

        @Test
        fun `When a too low year field is validated, then an 'OutOfRangeValue' issue is returned`() {
            val request = validPaperBuilder.setYear(-1).build()

            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Int>>(result)
        }

        @Test
        fun `When a too high year field is validated, then an 'OutOfRangeValue' issue is returned`() {
            val request = validPaperBuilder.setYear(LocalDate.now().year + 2).build()

            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Int>>(result)
        }

        @Test
        fun `When the authors list is too long, then a 'TooLongList' issue is returned`() {
            val authors = mutableListOf<PaperOuterClass.Author>()
            (1..MAX_AUTHOR_COUNT + 1).forEach { i ->
                authors.add(
                    author {
                        firstName = "John$i"
                        lastName = "Doe$i"
                    },
                )
            }
            val request = validPaperBuilder.clearAuthors().addAllAuthors(authors).build()

            val result = validateRequest(request)

            assertInvalidResult<TooLongList>(result)
        }

        @Test
        fun `When the authors list is empty, then no issue is returned`() {
            val request = validPaperBuilder.clearAuthors().build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When authors are invalid, then the issues are returned`() {
            val authors = listOf(
                author {
                    firstName = ""
                    lastName = ""
                },
            )
            val request = validPaperBuilder.clearAuthors().addAllAuthors(authors).build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isLeft()
        }
    }

    @Nested
    inner class UpdateRequest {
        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(
                listOf(
                    "paper.external_id",
                    "paper.title",
                    "paper.abstrakt",
                    "paper.year",
                    "paper.publisher",
                    "paper.publication_name",
                    "paper.publication_type",
                    "paper.authors",
                ),
            )

        private val validUpdateRequestBuilder: Paper.Update.Builder = Paper.Update.newBuilder()
            .setPaper(validPaperBuilder)
            .setMask(validFieldMask)

        private fun getExampleRequest(paper: Paper? = null, paths: List<String>? = null): Paper.Update {
            val builder = validUpdateRequestBuilder
            if (paper != null) builder.setPaper(paper)
            if (paths != null) builder.setMask(FieldMaskUtil.fromStringList(paths))
            return builder.build()
        }

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validUpdateRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @ParameterizedTest(
            name = "When a field mask is validated with {1}, then the 'InvalidFieldMask' issue is returned",
        )
        @MethodSource("se.uulm.snowballr.backend.validation.PaperValidatorTest#invalidFields")
        fun `When an invalid field mask is validated, then the 'InvalidFieldMask' issue is returned`(
            paths: List<String>,
            @Suppress("unused") testNameDescription: String,
        ) {
            val request = getExampleRequest(paths = paths)

            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When an invalid ID is validated, then the 'InvalidId' issue is returned`() {
            val paper = validPaperBuilder.setId("invalid-id").build()
            val request = getExampleRequest(paper)

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("se.uulm.snowballr.backend.validation.PaperValidatorTest#blankFieldTestProviderWithFieldMask")
        fun `When a blank field is validated, then expected result is returned`(
            fieldName: String,
            isFieldInMask: Boolean,
            @Suppress("unused") testName: String,
        ) {
            val fieldDescriptor = Paper.getDescriptor().findFieldByName(fieldName)
            val paper = validPaperBuilder.setField(fieldDescriptor, " ").build()
            val path = if (isFieldInMask) "paper.$fieldName" else "paper.id"
            val request = getExampleRequest(paper, listOf(path))

            val result = validateRequest(request)

            if (isFieldInMask) {
                assertInvalidResult<BlankField>(result)
            } else {
                EitherAssert.assertThat(result).isRight()
            }
        }

        @ParameterizedTest(name = "{3}")
        @MethodSource("se.uulm.snowballr.backend.validation.PaperValidatorTest#tooLongFieldTestProviderWithFieldMask")
        fun `When a too long field is validated, then expected result is returned`(
            fieldName: String,
            maxLength: Int,
            isFieldInMask: Boolean,
            @Suppress("unused") testName: String,
        ) {
            val fieldDescriptor = Paper.getDescriptor().findFieldByName(fieldName)
            val paper = validPaperBuilder.setField(fieldDescriptor, "a".repeat(maxLength + 1)).build()
            val path = if (isFieldInMask) "paper.$fieldName" else "paper.id"
            val request = getExampleRequest(paper, listOf(path))

            val result = validateRequest(request)

            if (isFieldInMask) {
                assertInvalidResult<TooLongField>(result)
            } else {
                EitherAssert.assertThat(result).isRight()
            }
        }

        @Test
        fun `When a too low year field is validated, then an 'OutOfRangeValue' issue is returned`() {
            val paper = validPaperBuilder.setYear(YEAR_MIN_VALUE - 1).build()
            val request = getExampleRequest(paper, listOf("paper.year"))
            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Int>>(result)
        }

        @Test
        fun `When a too high year field is validated, then an 'OutOfRangeValue' issue is returned`() {
            val paper = validPaperBuilder.setYear(LocalDate.now().year + 2).build()
            val request = getExampleRequest(paper, listOf("paper.year"))

            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Int>>(result)
        }

        @Test
        fun `When the authors list is too long, then a 'TooLongList' issue is returned`() {
            val authors = mutableListOf<PaperOuterClass.Author>()
            (1..MAX_AUTHOR_COUNT + 1).forEach { i ->
                authors.add(
                    author {
                        firstName = "John$i"
                        lastName = "Doe$i"
                    },
                )
            }
            val paper = validPaperBuilder.clearAuthors().addAllAuthors(authors).build()
            val request = getExampleRequest(paper, listOf("paper.authors"))

            val result = validateRequest(request)

            assertInvalidResult<TooLongList>(result)
        }

        @Test
        fun `When the authors list is empty, then no issue is returned`() {
            val paper = validPaperBuilder.clearAuthors().build()
            val request = getExampleRequest(paper, listOf("paper.authors"))

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When authors are invalid, then the issues are returned`() {
            val authors = listOf(
                author {
                    firstName = ""
                    lastName = ""
                },
            )
            val paper = validPaperBuilder.clearAuthors().addAllAuthors(authors).build()
            val request = getExampleRequest(paper, listOf("paper.authors"))

            val result = validateRequest(request)

            assertInstanceOf<Either.Left<NonEmptyList<ValidationIssue>>>(result)
            val issues = result.value.toList()
            assertThat(issues).hasSize(1)
            val compositeIssue = issues[0]
            assertInstanceOf<CompositeIssue>(compositeIssue)
            assertThat("$compositeIssue").startsWith("Issues of author at index 0")
        }

        @Test
        fun `When the externalId is empty, then no issue is returned`() {
            val paper = validPaperBuilder.setExternalId("").build()
            val request = getExampleRequest(paper, listOf("paper.external_id"))

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the externalId is blank, then a 'BlankField' issue is returned`() {
            val paper = validPaperBuilder.setExternalId("   ").build()
            val request = getExampleRequest(paper, listOf("paper.external_id"))

            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }
    }
}
