package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IExportService
import java.util.UUID

@RestController
@RequestMapping(Routes.EXPORT_ROUTE)
class ExportController(private val exportService: IExportService) {
    @GetMapping("/formats")
    fun getAvailableExportFormats(): Set<ExportFormat> = onRequest { exportService.getAvailableExportFormats() }

    @GetMapping("/projects/{id}")
    fun exportProject(@PathVariable id: UUID, @RequestParam format: ExportFormat): ResponseEntity<ByteArray> =
        onRequest {
            val export = exportService.exportProject(id, format)

            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(export.filename).build().toString(),
                )
                .body(export.data)
        }
}
