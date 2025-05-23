package se.uulm.snowballr.backend.utils

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.Objects.requireNonNull
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass

/**
 * Annotation for parameterized testing of gRPC enums. This annotation automatically
 * provides all valid enum values, excluding the "UNRECOGNIZED" value, to the annotated test method.
 *
 * The annotation is combined with JUnit's [ParameterizedTest] to facilitate testing against
 * all valid enum values of a specified enum class. The associated arguments are provided
 * using the [GenericEnumProvider], which filters out invalid or unrecognized enum constants.
 *
 * This is the same as the following code:
 * ```
 * @ParameterizedTest
 * @EnumSource(
 *     value = ExampleEnum::class,
 *     names = ["UNRECOGNIZED"],
 *     mode = EnumSource.Mode.EXCLUDE,
 * )
 * fun `When ..., then ...`(value: ExampleEnum) {
 *     // test logic
 * }
 * ```
 *
 * @property value The enum class to be tested. This must be a class that extends [Enum].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest
@ArgumentsSource(GenericEnumProvider::class)
annotation class GrpcEnumSourceTest(
    val value: KClass<out Enum<*>>,
)

class GenericEnumProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        val method = checkNotNull(context.testMethod.getOrNull()) { "No test method found." }

        val annotation = method.getAnnotation(GrpcEnumSourceTest::class.java)
        requireNonNull(annotation, "Missing @GrpcEnumSourceTest annotation.")

        val enumClass = annotation.value.java
        require(enumClass.isEnum) { "Provided class ${enumClass.name} is not an enum." }

        @Suppress("UNCHECKED_CAST")
        val values = enumClass.enumConstants as Array<Enum<*>>
        // Filter out UNRECOGNIZED, since it is not a valid value for the enum
        return values.filter { it.toString() !== "UNRECOGNIZED" }.map { Arguments.of(it) }.stream()
    }
}
