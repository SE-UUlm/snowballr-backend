package se.uulm.snowballr.backend.model.export

/**
 * Data class that represents a file that has been exported.
 */
data class FileExport(
    val data: ByteArray,
    val filename: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileExport) return false

        if (!data.contentEquals(other.data)) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + filename.hashCode()
        return result
    }
}
