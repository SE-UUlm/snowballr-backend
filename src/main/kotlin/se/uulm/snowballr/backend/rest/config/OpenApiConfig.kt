package se.uulm.snowballr.backend.rest.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun snowballROpenApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("SnowballR API")
            .version("v1")
            .description("REST API for the SnowballR Application"),
    )
}
