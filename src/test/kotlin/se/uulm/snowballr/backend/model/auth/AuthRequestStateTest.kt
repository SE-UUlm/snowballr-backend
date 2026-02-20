package se.uulm.snowballr.backend.model.auth

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthRequestStateTest {
    @Test
    fun `When all values are provided, then they are stored in the state`() {
        val call = mockk<ServerCall<String?, String?>>(relaxed = true)
        val headers = Metadata()
        val next = mockk<ServerCallHandler<String?, String?>>(relaxed = true)

        val state = AuthRequestState(call, headers, next)

        assertEquals(call, state.call)
        assertEquals(headers, state.headers)
        assertEquals(next, state.next)
    }

    @Test
    fun `When optional values are null, then they remain null in the state`() {
        val call = mockk<ServerCall<String?, String?>>(relaxed = true)

        val state = AuthRequestState<String, String>(call, null, null)

        assertEquals(call, state.call)
        assertNull(state.headers)
        assertNull(state.next)
    }
}
