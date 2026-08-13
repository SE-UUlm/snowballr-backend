package se.uulm.snowballr.backend.rest

import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import se.uulm.snowballr.backend.RandomKeyGenerator
import se.uulm.snowballr.backend.TestDatabase
import se.uulm.snowballr.backend.accessCheckerDeps
import se.uulm.snowballr.backend.auth.AuthenticationManager
import se.uulm.snowballr.backend.auth.CookieManager
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.auth.JwtManager
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.fetcher.IFetcherOrchestrator
import se.uulm.snowballr.backend.mail.EmailManager
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.mailServiceDeps
import se.uulm.snowballr.backend.repository.RepositoryHelper
import se.uulm.snowballr.backend.repositoryLayerDeps
import se.uulm.snowballr.backend.rest.controllers.Routes
import se.uulm.snowballr.backend.serviceLayerDeps

/**
 * Drives the full auth workflow (register, verify email, login, check status, change password, logout) through
 * the actual REST endpoints and the real Spring Security filter chain - not by calling the service layer
 * directly - so that cookie handling in [se.uulm.snowballr.backend.rest.config.RequestContextFilter] is
 * exercised the same way a real client would.
 *
 * Koin is started once in [startTestKoin], before the Spring context is created, since `KoinBridge` and
 * `SecurityConfig` resolve Koin-backed beans eagerly during context refresh (see
 * [se.uulm.snowballr.backend.openapi.OpenApiContractTest] for the same requirement). Unlike
 * [se.uulm.snowballr.backend.integration.IntegrationTest], this runs against a single shared database for the
 * lifetime of the class, since there is only one, sequential, test method here.
 */
@SpringBootTest(classes = [SnowballRApplication::class])
@AutoConfigureMockMvc
@Tag("integration")
class AuthWorkflowIntegrationTest(@Autowired private val mvc: MockMvc) {
    @Test
    fun `When a user registers, verifies, logs in, checks status, changes password, and logs out, then each step succeeds`() {
        val email = "workflow-user@example.com"
        val password = "SecureP@ssw0rd!"
        val newPassword = "EvenMoreSecureP@ssw0rd!"

        val verificationToken = slot<String>()
        every {
            emailManagerMock.createVerificationLink(capture(verificationToken))
        } returns "https://example.com/verify"
        coJustRun { emailManagerMock.sendVerificationEmail(any(), any()) }

        mvc.perform(
            post(Routes.USERS_ROUTE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"firstName":"Workflow","lastName":"User","email":"$email","password":"$password"}""",
                ),
        ).andExpect(status().isCreated)

        mvc.perform(post("${Routes.AUTH_ROUTE}/verify-email").param("token", verificationToken.captured))
            .andExpect(status().isOk)

        val loginResult = mvc.perform(
            post("${Routes.AUTH_ROUTE}/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}"""),
        ).andExpect(status().isOk).andReturn()

        val setCookieHeaders = loginResult.response.getHeaders("Set-Cookie")
        val accessToken = requireNotNull(extractCookieValue(setCookieHeaders, "access_token")) {
            "Login response did not set an access_token cookie"
        }
        val refreshToken = requireNotNull(extractCookieValue(setCookieHeaders, "refresh_token")) {
            "Login response did not set a refresh_token cookie"
        }
        val sessionCookieHeader = "access_token=$accessToken; refresh_token=$refreshToken"

        mvc.perform(get("${Routes.AUTH_ROUTE}/status").header("Cookie", sessionCookieHeader))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("AUTHENTICATED")))

        mvc.perform(
            post("${Routes.AUTH_ROUTE}/change-password")
                .header("Cookie", sessionCookieHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"oldPassword":"$password","newPassword":"$newPassword"}"""),
        ).andExpect(status().isOk)

        val logoutResult = mvc.perform(post("${Routes.AUTH_ROUTE}/logout").header("Cookie", sessionCookieHeader))
            .andExpect(status().isOk)
            .andReturn()

        val logoutCookieHeaders = logoutResult.response.getHeaders("Set-Cookie")
        assertEquals("", extractCookieValue(logoutCookieHeaders, "access_token"), "logout did not clear access_token")
        assertEquals("", extractCookieValue(logoutCookieHeaders, "refresh_token"), "logout did not clear refresh_token")

        // No Cookie header this time: a real browser would have dropped the cookies logout just cleared.
        mvc.perform(get("${Routes.AUTH_ROUTE}/status")).andExpect(status().isUnauthorized)
    }

    /**
     * Extracts the value of the cookie called [name] from a list of raw `Set-Cookie` header strings
     * (e.g. `"access_token=abc123; Max-Age=900; Path=/; SameSite=Strict; HttpOnly; Secure"`).
     */
    private fun extractCookieValue(setCookieHeaders: List<String>, name: String): String? =
        setCookieHeaders.firstOrNull { it.startsWith("$name=") }?.run {
            removePrefix("$name=").substringBefore(";")
        }

    private companion object {
        val db = TestDatabase()
        val envReaderMock = mockk<EnvReader>()
        val emailManagerMock = mockk<EmailManager>()
        val fetcherManagerMock = mockk<IFetcherManager>()
        val fetcherOrchestratorMock = mockk<IFetcherOrchestrator>()

        @JvmStatic
        @BeforeAll
        fun startTestKoin() {
            db.setUp()
            RepositoryHelper.db = db
            db.setUpTest(needsTestUser = false) {}

            val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()
            every { envReaderMock.env.encryption } returns Env.Encryption(privateKeyBase64, publicKeyBase64)
            every { envReaderMock.env.lifetime } returns Env.Lifetime(30, 7, 1)
            every { envReaderMock.env.miscellaneous } returns Env.Miscellaneous(
                logLevel = "INFO",
                authBypassEnabled = false,
                frontendBaseUrl = "http://localhost:3000",
            )
            coJustRun { fetcherOrchestratorMock.enqueue(any()) }

            startKoin {
                modules(
                    module {
                        single<IDatabase> { db }
                        single<EnvReader> { envReaderMock }
                        single<IEmailManager> { emailManagerMock }
                        single<IFetcherManager> { fetcherManagerMock }
                        single<IFetcherOrchestrator> { fetcherOrchestratorMock }

                        repositoryLayerDeps()
                        mailServiceDeps()
                        accessCheckerDeps()
                        serviceLayerDeps()

                        singleOf(::JwtManager) {
                            createdAtStart()
                            bind<IJwtManager>()
                        }
                        singleOf(::CookieManager) { bind<ICookieManager>() }
                        singleOf(::AuthenticationManager) { bind<IAuthenticationManager>() }
                    },
                )
            }
        }

        @JvmStatic
        @AfterAll
        fun stopTestKoin() {
            db.tearDownTest()
            db.tearDown()
            db.close()
            stopKoin()
        }
    }
}
