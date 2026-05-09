package se.uulm.snowballr.backend.service.export

import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.export.ExportFormat

class GetAvailableExportFormatsTest : ExportServiceTest() {
    @Test
    fun `When available export formats are requested, then no exception is thrown`() = runTest {
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns ExportFormat.entries.toSet()

        assertDoesNotThrow { service.getAvailableExportFormats() }
    }
}
