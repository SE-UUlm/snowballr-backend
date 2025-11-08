package se.uulm.snowballr.backend.export

import kotlinx.serialization.json.Json
import se.uulm.snowballr.backend.model.export.ProjectExport

/**
 * Exports a [ProjectExport] to the JSON format.
 */
class JsonExporter : IExporter {
    private val json = Json

    override fun export(export: ProjectExport): ByteArray {
        val jsonString = json.encodeToString<ProjectExport>(export)
        return jsonString.toByteArray()
    }

    override fun getExtension() = "json"
}
