package se.uulm.snowballr.backend.model.dto

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PdfTest {
    @Test
    fun `When comparing with itself, then Pdf is equal`() {
        val pdf = Pdf(UUID.randomUUID(), byteArrayOf(1, 2, 3))

        assertEquals(pdf, pdf)
    }

    @Test
    fun `When comparing with a non Pdf object, then it is not equal`() {
        val pdf = Pdf(UUID.randomUUID(), byteArrayOf(1, 2, 3))

        assertFalse(pdf.equals("not-a-pdf"))
    }

    @Test
    fun `When id and data are equal, then equals and hashCode match`() {
        val id = UUID.randomUUID()
        val left = Pdf(id, byteArrayOf(1, 2, 3))
        val right = Pdf(id, byteArrayOf(1, 2, 3))

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
    }

    @Test
    fun `When id differs, then Pdf is not equal`() {
        val left = Pdf(UUID.randomUUID(), byteArrayOf(1, 2, 3))
        val right = Pdf(UUID.randomUUID(), byteArrayOf(1, 2, 3))

        assertNotEquals(left, right)
    }

    @Test
    fun `When data differs, then Pdf is not equal`() {
        val id = UUID.randomUUID()
        val left = Pdf(id, byteArrayOf(1, 2, 3))
        val right = Pdf(id, byteArrayOf(9, 2, 3))

        assertNotEquals(left, right)
    }
}
