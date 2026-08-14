package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
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
        )

        @JvmStatic
        fun validExternalIds() = ExternalIdType.entries.map {
            externalId {
                type = it.name
                value = VALID_EXTERNAL_ID_VALUES.getValue(it)
            }
        }

        @JvmStatic
        fun validArxivIdOldStyle() = externalId {
            type = ExternalIdType.ARXIV.name
            value = "hep-th/9901001"
        }

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
        fun invalidExternalIdsFormatValue() = INVALID_FORMAT_VALUES.map { (type, value) ->
            externalId {
                this.type = type.name
                this.value = value
            }
        }
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#validExternalIds")
    fun `When an external ID is valid, then no issues are returned`(externalId: ExternalId) {
        val result = validateExternalId(externalId)

        EitherAssert.assertThat(result).isRight()
    }

    @Test
    fun `When an ArXiv ID uses the old-style format, then no issues are returned`() {
        val result = validateExternalId(validArxivIdOldStyle())

        EitherAssert.assertThat(result).isRight()
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#invalidExternalIdsTooLongValue")
    fun `When an external ID has a too long value, then a 'TooLongField' issue is returned`(externalId: ExternalId) {
        val result = validateExternalId(externalId)

        assertInvalidResult<TooLongField>(result)
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#invalidExternalIdsBlankValue")
    fun `When an external ID has a blank value, then a 'BlankField' issue is returned`(externalId: ExternalId) {
        val result = validateExternalId(externalId)

        assertInvalidResult<BlankField>(result)
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#invalidExternalIdsFormatValue")
    fun `When an external ID does not match the format of its type, then a 'InvalidExternalIdFormat' issue is returned`(
        externalId: ExternalId,
    ) {
        val result = validateExternalId(externalId)

        assertInvalidResult<InvalidExternalIdFormat>(result)
    }

    @Test
    fun `When an ACL external ID has a non-blank value of valid length, then no issues are returned regardless of format`() {
        val externalId = externalId {
            type = ExternalIdType.ACL.name
            value = "this is not a real ACL anthology id but should still be accepted"
        }

        val result = validateExternalId(externalId)

        EitherAssert.assertThat(result).isRight()
    }

    @Test
    fun `When a DBLP external ID has a non-blank value of valid length, then no issues are returned regardless of format`() {
        val externalId = externalId {
            type = ExternalIdType.DBLP.name
            value = "this is not a real dblp key but should still be accepted"
        }

        val result = validateExternalId(externalId)

        EitherAssert.assertThat(result).isRight()
    }

    @Test
    fun `When an external ID has an unknown type, then a 'InvalidEnumValue' issue is returned`() {
        val externalId = ExternalId.newBuilder()
            .setType("UNKNOWN")
            .setValue("some value")
            .build()

        val result = validateExternalId(externalId)

        assertInvalidResult<InvalidEnumValue>(result)
    }
}
