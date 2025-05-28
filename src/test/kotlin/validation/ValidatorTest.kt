package se.uulm.snowballr.backend.validation

import arrow.core.Either
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.UnknownRequest

class ValidatorTest {
    @Test
    fun `When an unknown request is validated, then the 'UnknownRequest' issue is returned`() {
        val result = validateRequest(object {})

        EitherAssert.assertThat(result).isLeft()
        val value = (result as Either.Left).value
        assertThat(value.size).isEqualTo(1)
        val issue = value.first()
        assertThat(issue).isInstanceOf(UnknownRequest::class.java)
    }
}
