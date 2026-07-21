package se.uulm.snowballr.backend.export

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.export.testdata.emptyProjectExport
import se.uulm.snowballr.backend.export.testdata.fullProjectExport
import se.uulm.snowballr.backend.model.export.ProjectExport

class JsonExporterTest {
    companion object {
        @JvmStatic
        fun exportExamples() = listOf(
            Arguments.of(emptyProjectExport, "export-test-files/json/emptyProjectExport.json"),
            Arguments.of(fullProjectExport, "export-test-files/json/fullProjectExport.json"),
        )
    }

    val json = Json

    @ParameterizedTest(name = "When a project is exported, then a valid JSON file is produced matching {1}")
    @MethodSource("exportExamples")
    fun `When a project is exported, then a valid JSON is produced`(
        projectExport: ProjectExport,
        expectedFile: String,
    ) {
        val url = this::class.java.classLoader.getResource(expectedFile)
        val expectedStringRaw = url?.readText().orEmpty()
        // Do round-trip test to check validity of serialization and remove pretty printing
        val expectedString = json.encodeToString(json.decodeFromString<ProjectExport>(expectedStringRaw))

        val actualString = String(JsonExporter().export(projectExport))

        assertEquals(expectedString, actualString, "JSON export does not match expected JSON")
    }
}
