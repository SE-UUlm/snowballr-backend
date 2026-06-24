package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import snowballr.PaperOuterClass.Paper.ExternalId

object ExternalIdValidator {
    const val EXTERNAL_ID_MAX_LENGTH = 100

    fun validateExternalId(externalId: ExternalId): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureValidEnumValue<ExternalIdType>(externalId.type, "External ID Type") },
            { ensureTextFieldValidity("value", externalId.value, EXTERNAL_ID_MAX_LENGTH) },
        ) { _, _ -> }
    }
}
