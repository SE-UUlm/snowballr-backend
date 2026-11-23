package se.uulm.snowballr.backend.rest

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.rest.controllers.Routes
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.IUserService

/**
 * Checks that the REST API and its docs are not served unless [REST_API_ENABLED_PROPERTY] is set, while the actuator
 * health endpoint stays available. The property is deliberately not set here, so the default is tested.
 *
 * See [se.uulm.snowballr.backend.openapi.OpenApiContractTest] for why Koin is started with mocks.
 */
@SpringBootTest(
    classes = [SnowballRApplication::class],
    // Bind the gRPC server to a free port, so the test doesn't depend on 9090 being available
    properties = ["spring.grpc.server.port=0"],
)
@AutoConfigureMockMvc
class RestApiFeatureFlagTest(@Autowired private val mvc: MockMvc) {
    @Test
    fun `When the REST API is disabled by default, then a public REST endpoint is not found`() {
        mvc.perform(post("${Routes.AUTH_ROUTE}/login"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `When the REST API is disabled by default, then the OpenAPI docs are not found`() {
        mvc.perform(get("/api-docs"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `When the REST API is disabled by default, then the Swagger UI is not found`() {
        mvc.perform(get("/swagger"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `When the REST API is disabled by default, then the health endpoint is still available`() {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    private companion object {
        @JvmStatic
        @BeforeAll
        fun startTestKoin() {
            val envReader = mockk<EnvReader>()
            every { envReader.env.miscellaneous.authBypassEnabled } returns true

            startKoin {
                modules(
                    module {
                        single<IProjectService> { mockk() }
                        single<IAuthenticationService> { mockk() }
                        single<IAuthenticationManager> { mockk() }
                        single<ICookieManager> { mockk() }
                        single<EnvReader> { envReader }
                        single<IUserService> { mockk() }
                    },
                )
            }
        }

        @JvmStatic
        @AfterAll
        fun stopTestKoin() = stopKoin()
    }
}
