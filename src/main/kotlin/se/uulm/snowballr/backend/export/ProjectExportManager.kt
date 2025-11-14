package se.uulm.snowballr.backend.export

import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.ProjectPaperFull
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.FileExport
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Manager responsible for exporting projects in various formats.
 */
object ProjectExportManager {
    private val exporters = mapOf<ExportFormat, IExporter>(
        ExportFormat.JSON to JsonExporter(),
    )

    fun getSupportedFormats(): Set<ExportFormat> = exporters.keys

    fun exportProject(
        format: ExportFormat,
        project: Project,
        projectMembers: List<ProjectMemberWithUser>,
        projectPapers: List<ProjectPaperFull>,
        projectCriteria: List<Criterion>,
    ): FileExport {
        val exporter = exporters[format] ?: throw IllegalArgumentException("Unsupported export format: $format")

        val builder = ProjectExportBuilder(project, projectMembers, projectPapers, projectCriteria)
        val projectExport = builder.buildExport()

        val data = exporter.export(projectExport)
        val filename = createFilename(project.name, exporter.getExtension())
        return FileExport(data, filename)
    }

    private fun createFilename(projectName: String, fileExtension: String): String {
        val timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        return sanitizeFilename("$projectName-$timestamp.$fileExtension")
    }

    private fun sanitizeFilename(input: String): String {
        // Define characters not allowed in filenames (Windows + UNIX safe set) + white-space
        val illegalChars = """[\\/:*?"<>|\p{Cntrl}\s]""".toRegex()

        var sanitized = input.replace(illegalChars, "_")

        // Trim spaces and dots (Windows doesn't like names ending with . or space)
        sanitized = sanitized.trim().trimEnd('.')

        // Fallback for empty filenames
        if (sanitized.isEmpty()) sanitized = "unnamed"

        return sanitized
    }
}
