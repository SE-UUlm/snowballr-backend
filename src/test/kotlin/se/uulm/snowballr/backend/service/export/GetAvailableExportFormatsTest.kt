package se.uulm.snowballr.backend.service.export

import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.export.ExportFormat

class GetAvailableExportFormatsTest : ExportServiceTest() {
    @Test
    fun `When available export formats are requested, then the correct values are returned`() = runTest {
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns ExportFormat.entries.toSet()

        val formats = service.getAvailableExportFormats()

        assertEquals(ExportFormat.entries.toSet(), formats)
    }
}
