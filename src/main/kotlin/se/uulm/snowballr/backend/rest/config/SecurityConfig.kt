package se.uulm.snowballr.backend.rest.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.rest.controllers.Routes

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationManager: IAuthenticationManager,
    private val cookieManager: ICookieManager,
    private val envReader: EnvReader,
) {
    @Bean
    fun restSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val edgeFilter = RequestContextFilter(authenticationManager, cookieManager, envReader)
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // Public Endpoints
                it.requestMatchers(HttpMethod.POST, Routes.USERS_ROUTE).permitAll()
                it.requestMatchers(HttpMethod.POST, "${Routes.AUTH_ROUTE}/login").permitAll()
                it.requestMatchers(HttpMethod.POST, "${Routes.AUTH_ROUTE}/verify-email").permitAll()
                it.requestMatchers(HttpMethod.GET, "${Routes.AUTH_ROUTE}/status").permitAll()
                it.requestMatchers(HttpMethod.POST, "${Routes.AUTH_ROUTE}/logout").permitAll()

                // Infra / docs endpoints are public.
                it.requestMatchers(
                    "/actuator/**",
                    "/api-docs",
                    "/api-docs.yaml",
                    "/swagger/**",
                    "/swagger-ui/**",
                    "/api-docs/swagger-config",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(unauthorizedEntryPoint()) }
            .addFilterBefore(edgeFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun unauthorizedEntryPoint(): AuthenticationEntryPoint = AuthenticationEntryPoint { _, response, _ ->
        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session is invalid")
    }
}
