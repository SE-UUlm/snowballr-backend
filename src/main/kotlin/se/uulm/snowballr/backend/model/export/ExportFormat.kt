package se.uulm.snowballr.backend.model.export

import snowballr.Export

/**
 * Formats used for exporting data.
 */
enum class ExportFormat {
    JSON,
}

fun Set<ExportFormat>.toGrpc(): Export.AvailableExportFormatsResponse = Export.AvailableExportFormatsResponse
    .newBuilder()
    .addAllFormats(this.map { it.toString() })
    .build()
