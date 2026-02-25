package se.uulm.snowballr.backend.model.export

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class FileExportTest {
    @Test
    fun `When comparing with itself, then FileExport is equal`() {
        val fileExport = FileExport(byteArrayOf(1, 2, 3), "result.json")

        assertEquals(fileExport, fileExport)
    }

    @Test
    fun `When comparing with a non FileExport object, then it is not equal`() {
        val fileExport = FileExport(byteArrayOf(1, 2, 3), "result.json")

        assertFalse(fileExport.equals("result.json"))
    }

    @Test
    fun `When data and filename are equal, then equals and hashCode match`() {
        val left = FileExport(byteArrayOf(1, 2, 3), "result.json")
        val right = FileExport(byteArrayOf(1, 2, 3), "result.json")

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
    }

    @Test
    fun `When data differs, then FileExport is not equal`() {
        val left = FileExport(byteArrayOf(1, 2, 3), "result.json")
        val right = FileExport(byteArrayOf(9, 2, 3), "result.json")

        assertNotEquals(left, right)
    }

    @Test
    fun `When filename differs, then FileExport is not equal`() {
        val left = FileExport(byteArrayOf(1, 2, 3), "result.json")
        val right = FileExport(byteArrayOf(1, 2, 3), "other.json")

        assertNotEquals(left, right)
    }
}
