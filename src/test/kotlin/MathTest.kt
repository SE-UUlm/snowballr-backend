package se.uulm.snowballr.backend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MathTest {
    @Nested
    inner class Add {
        @Test
        fun `When two numbers are added together, then the result is correct`() {
            assertThat(add(1, 2)).isEqualTo(3)
        }
    }

    @Nested
    inner class Subtract {
        @Test
        fun `When two numbers are subtracted, then the result is correct`() {
            assertThat(subtract(3, 2)).isEqualTo(1)
        }
    }
}
