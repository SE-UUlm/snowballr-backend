package se.uulm.snowballr.backend.validation

import arrow.core.Either
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidEnumValue
import se.uulm.snowballr.backend.model.InvalidExternalIdFormat
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.validation.ExternalIdValidator.validateExternalId
import snowballr.PaperKt.externalId
import snowballr.PaperOuterClass.Paper.ExternalId

class ExternalIdTest {
    companion object {
        /**
         * A malformed example value per [ExternalIdType] that has a format check defined, i.e. is neither blank nor
         * too long, but does not match the expected format.
         */
        private val INVALID_FORMAT_VALUES: Map<ExternalIdType, String> = mapOf(
            ExternalIdType.DOI to "not-a-doi",
            ExternalIdType.ARXIV to "not-an-arxiv-id",
            ExternalIdType.MAG to "not-a-number",
            ExternalIdType.PUB_MED to "not-a-number",
            ExternalIdType.MEDLINE to "not-a-number",
            ExternalIdType.PUB_MED_CENTRAL to "1234567",
            ExternalIdType.SEMANTIC_SCHOLAR to "not-a-hash",
            ExternalIdType.ACL to "not-an-acl-id",
            ExternalIdType.DBLP to "not-a-dblp-key",
        )

        /**
         * Extra valid values per [ExternalIdType] beyond the single representative example in
         * [VALID_EXTERNAL_ID_VALUES], covering alternate schemes (old/new style, version suffixes) and values that
         * sit at the boundary of what the format regex allows.
         */
        private val EXTRA_VALID_VALUES: List<Pair<ExternalIdType, String>> = listOf(
            ExternalIdType.DOI to "10.48550/arXiv.2202.01037", // registrant code longer than 4 digits
            ExternalIdType.ARXIV to "2101.00001v2", // new-style with version suffix
            ExternalIdType.ARXIV to "hep-th/9901001", // legacy old-style
            ExternalIdType.ACL to "2021.acl-long.1", // new-style (2020 onward)
            ExternalIdType.DBLP to "journals/corr/abs-2103-05387", // hyphenated id-suffix, e.g. CoRR/arXiv keys
        )

        /**
         * Extra malformed values per [ExternalIdType] beyond [INVALID_FORMAT_VALUES], covering values that are
         * close to, but do not quite match, the expected format.
         */
        private val EXTRA_INVALID_FORMAT_VALUES: List<Pair<ExternalIdType, String>> = listOf(
            ExternalIdType.DOI to "10.100/xyz123", // registrant code shorter than 4 digits
            ExternalIdType.DOI to "10.1000xyz123", // missing separating slash
            ExternalIdType.ARXIV to "210.00001", // year-month segment shorter than 4 digits
            ExternalIdType.ACL to "P19-12345", // paper number longer than 4 digits
            ExternalIdType.DBLP to "journals/tods", // missing id-suffix segment
        )

        private fun argumentsFor(type: ExternalIdType, value: String): Arguments {
            val id = externalId {
                this.type = type.name
                this.value = value
            }
            return Arguments.of(id, "$type '$value'")
        }

        @JvmStatic
        fun validExternalIds(): List<Arguments> =
            (ExternalIdType.entries.map { it to VALID_EXTERNAL_ID_VALUES.getValue(it) } + EXTRA_VALID_VALUES)
                .map { (type, value) -> argumentsFor(type, value) }

        @JvmStatic
        fun invalidExternalIdsTooLongValue() = ExternalIdType.entries.map {
            externalId {
                type = it.name
                value = "a".repeat(ExternalIdValidator.EXTERNAL_ID_MAX_LENGTH + 1)
            }
        }

        @JvmStatic
        fun invalidExternalIdsBlankValue() = ExternalIdType.entries.map {
            externalId {
                type = it.name
                value = ""
            }
        }

        @JvmStatic
        fun invalidExternalIdsFormatValue(): List<Arguments> =
            (INVALID_FORMAT_VALUES.toList() + EXTRA_INVALID_FORMAT_VALUES)
                .map { (type, value) -> argumentsFor(type, value) }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#validExternalIds")
    fun `When an external ID is valid, then no issues are returned`(
        externalId: ExternalId,
        @Suppress("unused") testName: String,
    ) {
        val result = validateExternalId(externalId)

        EitherAssert.assertThat(result).isRight()
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#invalidExternalIdsTooLongValue")
    fun `When an external ID has a too long value, then a 'TooLongField' issue is returned`(externalId: ExternalId) {
        val result = validateExternalId(externalId)

        assertInvalidResult<TooLongField>(result)
        assertThat((result as Either.Left).value).hasSize(1)
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#invalidExternalIdsBlankValue")
    fun `When an external ID has a blank value, then a 'BlankField' issue is returned`(externalId: ExternalId) {
        val result = validateExternalId(externalId)

        assertInvalidResult<BlankField>(result)
        assertThat((result as Either.Left).value).hasSize(1)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#invalidExternalIdsFormatValue")
    fun `When an external ID does not match the format of its type, then a 'InvalidExternalIdFormat' issue is returned`(
        externalId: ExternalId,
        @Suppress("unused") testName: String,
    ) {
        val result = validateExternalId(externalId)

        assertInvalidResult<InvalidExternalIdFormat>(result)
        assertThat((result as Either.Left).value).hasSize(1)
    }

    @Test
    fun `When an external ID has an unknown type, then a 'InvalidEnumValue' issue is returned`() {
        val externalId = ExternalId.newBuilder()
            .setType("UNKNOWN")
            .setValue("some value")
            .build()

        val result = validateExternalId(externalId)

        assertInvalidResult<InvalidEnumValue>(result)
        assertThat((result as Either.Left).value).hasSize(1)
    }

    @Test
    fun `When an external ID is validated, then the value is trimmed before validating`() {
        val externalId = ExternalId.newBuilder()
            .setType(ExternalIdType.DOI.name)
            .setValue("   10.1000/xyz123    ")
            .build()

        val result = validateExternalId(externalId)

        EitherAssert.assertThat(result).isRight()
    }
}
