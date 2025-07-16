package se.uulm.snowballr.backend

import io.grpc.Context
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import se.uulm.snowballr.backend.auth.GrpcContext

private const val CONTEXT_KEY = "grpc_context"
private const val COOKIES_MAP_KEY = "grpc_cookies_map"

/**
 * A JUnit 5 Extension to automatically set up and tear down a [GrpcContext] for tests.
 * It also allows injecting the `cookiesToSet` map directly into test methods.
 */
class GrpcTestContextExtension : BeforeEachCallback, AfterEachCallback, ParameterResolver {
    override fun beforeEach(context: ExtensionContext) {
        val store = context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestMethod))

        val cookiesMap = mutableMapOf<String, String?>()
        val testContext = Context.current()
            .withValue(GrpcContext.COOKIES_TO_SET_CONTEXT_KEY, cookiesMap)

        val previousContext = testContext.attach()

        // Store the previous context and the cookie map for later use
        store.put(CONTEXT_KEY, previousContext)
        store.put(COOKIES_MAP_KEY, cookiesMap)
    }

    override fun afterEach(context: ExtensionContext) {
        val store = context.getStore(ExtensionContext.Namespace.create(javaClass, context.requiredTestMethod))
        val previousContext = store.get(CONTEXT_KEY, Context::class.java)

        // Detach our context to clean up
        previousContext?.let { Context.current().detach(it) }
    }

    /**
     * Indicates support for injecting a [MutableMap] parameter into test methods (i.e., the cookie map).
     */
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == MutableMap::class.java

    /**
     * Resolves the injectable parameter by returning the cookie map from the store.
     */
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val store =
            extensionContext.getStore(ExtensionContext.Namespace.create(javaClass, extensionContext.requiredTestMethod))
        return store.get(COOKIES_MAP_KEY, MutableMap::class.java)
    }
}
