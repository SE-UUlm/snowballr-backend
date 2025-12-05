package se.uulm.snowballr.backend.export

import se.uulm.snowballr.backend.export.ProjectExportManager.getSupportedFormats
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

    /**
     * Returns the set of supported export formats.
     */
    fun getSupportedFormats(): Set<ExportFormat> = exporters.keys

    /**
     * Exports the given project and its associated data in the specified format.
     *
     * @param format The desired export format.
     * @param project The project to export.
     * @param projectMembers The members associated with the project.
     * @param projectPapers The papers associated with the project.
     * @param projectCriteria The criteria associated with the project.
     * @return A [FileExport] containing the exported data and filename.
     * @throws IllegalArgumentException if the specified format is not supported. Check with [getSupportedFormats]
     * first.
     */
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

    /**
     * Creates a sanitized filename using the project name and current timestamp.
     *
     * @param projectName The name of the project.
     * @param fileExtension The file extension for the export format.
     * @return A sanitized filename in the format "{projectName}-{timestamp}.{fileExtension}".
     */
    private fun createFilename(projectName: String, fileExtension: String): String {
        val timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        return sanitizeFilename("$projectName-$timestamp.$fileExtension")
    }

    /**
     * Sanitizes a filename by removing or replacing illegal characters.
     *
     * This includes:
     * - Replacing characters not allowed in filenames (such as \ / : * ? " < > | and control characters) with
     * underscores.
     * - Trimming leading and trailing whitespace and dots.
     * - Providing a fallback name ("unnamed") if the resulting filename is empty.
     *
     * @param input The original filename.
     * @return A sanitized filename safe for use in file systems.
     */
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
