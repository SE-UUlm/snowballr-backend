package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidEnumValue
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.validation.ExternalIdValidator.validateExternalId
import snowballr.PaperKt.externalId
import snowballr.PaperOuterClass.Paper.ExternalId

class ExternalIdTest {
    companion object {
        @JvmStatic
        fun validExternalIds() = ExternalIdType.entries.map {
            externalId {
                type = it.name
                value = "${it.name}-value"
            }
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
    }

    @ParameterizedTest
    @MethodSource("se.uulm.snowballr.backend.validation.ExternalIdTest#validExternalIds")
    fun `When an external ID is valid, then no issues are returned`(externalId: ExternalId) {
        val result = validateExternalId(externalId)

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
