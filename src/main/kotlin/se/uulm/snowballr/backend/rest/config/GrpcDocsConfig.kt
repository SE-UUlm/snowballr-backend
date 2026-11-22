package se.uulm.snowballr.backend.rest.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Files
import java.nio.file.Path

/**
 * Serves the sabledocs-generated gRPC API documentation under [GRPC_DOCS_ROUTE].
 *
 * The documentation is only generated in the Docker image (see Dockerfile), so the routes are only registered if
 * [GRPC_DOCS_DIRECTORY] exists. Remove this together with the gRPC API.
 */
@Configuration
class GrpcDocsConfig : WebMvcConfigurer {
    private val hasDocs = Files.isDirectory(GRPC_DOCS_DIRECTORY)

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        if (!hasDocs) return
        registry.addResourceHandler("$GRPC_DOCS_ROUTE/**")
            .addResourceLocations(GRPC_DOCS_DIRECTORY.toUri().toString())
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        if (!hasDocs) return
        registry.addRedirectViewController(GRPC_DOCS_ROUTE, "$GRPC_DOCS_ROUTE/index.html")
        registry.addRedirectViewController("$GRPC_DOCS_ROUTE/", "$GRPC_DOCS_ROUTE/index.html")
    }

    companion object {
        const val GRPC_DOCS_ROUTE = "/grpc-docs"
        private val GRPC_DOCS_DIRECTORY: Path = Path.of("/app/grpc-docs")
    }
}
