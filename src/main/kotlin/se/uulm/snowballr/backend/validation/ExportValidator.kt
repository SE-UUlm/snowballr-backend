package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.UnsupportedExportFormat
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.model.export.ExportFormat
import snowballr.Export

object ExportValidator {
    fun validateExportRequest(request: Export.ExportRequest): EitherNel<ValidationIssue, Unit> = either {
        ensure(
            ProjectExportManager.getSupportedFormats().map(ExportFormat::toString).contains(request.format),
        ) { UnsupportedExportFormat(request.format) }
    }.toEitherNel()
}
