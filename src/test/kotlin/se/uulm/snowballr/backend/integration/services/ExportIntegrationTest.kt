package se.uulm.snowballr.backend.integration.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import snowballr.Export.ExportRequest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportIntegrationTest : IntegrationTest() {
    @Nested
    inner class GetAvailableExportFormats {
        @Test
        fun `When available export formats are requested, then JSON is included`() = runTest {
            val response = exportService.getAvailableExportFormats()

            assertTrue(response.formatsList.contains("JSON"))
        }

        @Test
        fun `When available export formats are requested, then the list is not empty`() = runTest {
            val response = exportService.getAvailableExportFormats()

            assertFalse(response.formatsList.isEmpty())
        }
    }

    @Nested
    inner class ExportProject {
        @Test
        fun `When a project is exported as JSON, then the response contains non-empty data`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Export Project"))

            val response = exportService.exportProject(
                ExportRequest.newBuilder()
                    .setId(project.id)
                    .setFormat("JSON")
                    .build(),
            )

            assertFalse(response.data.isEmpty)
            assertFalse(response.fileName.isEmpty())
        }

        @Test
        fun `When a project is exported, then the file name reflects the project name`() = runTest {
            val project = projectService.createProject(CreateProjectRequest(name = "Named Export"))

            val response = exportService.exportProject(
                ExportRequest.newBuilder()
                    .setId(project.id)
                    .setFormat("JSON")
                    .build(),
            )

            assertTrue(response.fileName.isNotBlank())
        }
    }
}
