package se.uulm.snowballr.backend.model.dto.paper

import se.uulm.snowballr.backend.table.PdfTable
import java.util.UUID

/**
 * DTO of [PdfTable].
 */
data class Pdf(
    val id: UUID,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pdf

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
