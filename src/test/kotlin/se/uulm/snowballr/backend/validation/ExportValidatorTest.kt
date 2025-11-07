package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.export.ProjectExportManager
import se.uulm.snowballr.backend.model.UnsupportedExportFormat
import se.uulm.snowballr.backend.model.export.ExportFormat
import snowballr.Export.ExportRequest

class ExportValidatorTest {
    @Test
    fun `When validating an export request with a supported format, then no issue is returned`() {
        val supportedFormat = ExportFormat.JSON
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(supportedFormat)

        val request = ExportRequest.newBuilder()
            .setFormat(supportedFormat.toString())
            .build()

        val result = validateRequest(request)

        EitherAssert.assertThat(result).isRight()
    }

    @Test
    fun `When validating an export request with an unsupported format, then an 'UnsupportedExportFormat' issue is returned`() {
        val supportedFormat = ExportFormat.JSON
        mockkObject(ProjectExportManager)
        every { ProjectExportManager.getSupportedFormats() } returns setOf(supportedFormat)

        val request = ExportRequest.newBuilder()
            .setFormat("UNSUPPORTED_FORMAT")
            .build()

        val result = validateRequest(request)

        assertInvalidResult<UnsupportedExportFormat>(result)
    }
}
