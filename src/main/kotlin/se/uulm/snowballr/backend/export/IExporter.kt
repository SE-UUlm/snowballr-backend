package se.uulm.snowballr.backend.export

import se.uulm.snowballr.backend.model.export.ProjectExport

fun interface IExporter {
    /**
     * Exports the given [ProjectExport] to a byte array in the implementor's format.
     *
     * @param export The project export data to be exported.
     * @return A byte array representing the exported project data.
     */
    fun export(export: ProjectExport): ByteArray
}
