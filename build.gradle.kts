import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.shadow.jar)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.spring)
    alias(libs.plugins.spring.framework.boot)
    alias(libs.plugins.spring.dep.management)
    alias(libs.plugins.openapi.generator)
    application
}

group = "se.uulm.snowballr.backend"
version = "0.0.0"

gitVersioning.apply {
    refs {
        tag("v(?<version>\\d+\\.\\d+\\.\\d+)") {
            version = $$"${ref.version}"
        }
        // Beta channel: rolling next-release preview published on every merge to develop.
        branch("develop") {
            version =
                $$"${describe.tag.version.major}.${describe.tag.version.minor}.${describe.tag.version.patch.next}-beta.${commit.short}"
        }
        // Alpha channel: per-push preview for any other branch (e.g. feature branches backing an open PR). Must be
        // listed after the "develop" branch matcher, since the first matching entry wins.
        branch(".*") {
            version =
                $$"${describe.tag.version.major}.${describe.tag.version.minor}.${describe.tag.version.patch.next}-alpha.${commit.short}"
        }
    }

    // optional fallback configuration in case of no matching ref configuration (e.g. detached HEAD without a
    // matching branch, such as a checkout that isn't on a real branch ref)
    rev {
        version = $$"${commit}"
    }
}

application {
    mainClass.set("se.uulm.snowballr.backend.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
    implementation(libs.kotlin.logging)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.quartz.scheduler)
    implementation(libs.password4j)
    implementation(libs.jjwt.api)
    implementation(libs.nanoid)
    implementation(libs.dotenv.kotlin)
    implementation(libs.arrow.core)
    implementation(libs.grpc.kotlin)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.services)
    implementation(libs.protobuf.kotlin)
    implementation(libs.protobuf.java.util)
    implementation(libs.simple.java.mail)
    implementation(libs.handlebars)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.doc)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.grpc.server)

    testImplementation(libs.koin.test)
    testImplementation(libs.testcontainers)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertj.core)
    testImplementation(libs.assertj.arrow.core)
    testImplementation(libs.mockk)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.greenmail.junit5)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.spring.boot.test)

    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testRuntimeOnly(libs.junit.platform)

    detektPlugins(libs.detekt.formatting)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            // Handle nullability annotations as strict
            "-Xjsr305=strict",
        )
    }
    jvmToolchain(25)
}

val fetcherVenvDir = layout.projectDirectory.dir(".venv").asFile
val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val fetcherVenvPython: String = if (isWindows) {
    fetcherVenvDir.resolve("Scripts/python.exe").absolutePath
} else {
    fetcherVenvDir.resolve("bin/python3").absolutePath
}

/**
 * Resolve uv executable explicitly since an IDE might not be able to access it.
 *
 * The order is as follows:
 * - override via `UV_EXECUTABLE`
 * - search in `PATH`
 * - fallback to common install locations.
 */
val uvExecutable: String by lazy {
    val executableName = if (isWindows) "uv.exe" else "uv"
    val override = System.getenv("UV_EXECUTABLE")?.takeIf { it.isNotBlank() }
    val pathDirs = System.getenv("PATH").orEmpty().split(File.pathSeparatorChar)
    val homeDir = System.getProperty("user.home")
    val fallbackDirs = listOf(
        "$homeDir/.local/bin",
        "$homeDir/.cargo/bin",
        "/usr/local/bin",
        "/opt/homebrew/bin",
    )

    override
        ?: (pathDirs + fallbackDirs)
            .asSequence()
            .map { File(it, executableName) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
        ?: executableName
}

tasks.register<Exec>("createFetcherVenv") {
    group = "application"
    description = "Creates the local Python virtual environment for fetcher plugins."
    workingDir = projectDir
    commandLine(uvExecutable, "venv", "--allow-existing", ".venv")
}

tasks.register<Exec>("syncFetcherPythonDeps") {
    group = "application"
    description = "Synchronizes Python fetcher dependencies from requirements.txt using uv."
    dependsOn("createFetcherVenv")
    workingDir = projectDir
    commandLine(uvExecutable, "pip", "install", "--python", fetcherVenvPython, "-r", "requirements.txt")
}

tasks.register("setupFetcherPython") {
    group = "application"
    description = "Prepares Python tooling required by fetcher plugins."
    dependsOn("syncFetcherPythonDeps")
}

tasks.shadowJar {
    archiveClassifier.set("") // omit the "all" suffix

    // Several dependency jars (spring-boot, spring-boot-autoconfigure, spring-boot-tomcat,
    // spring-boot-actuator-autoconfigure, springdoc, ...) each ship their own
    // META-INF/spring.factories and META-INF/spring/*.imports files at the same path. Shadow's default
    // duplicatesStrategy is EXCLUDE, which drops every same-path entry but the first before transformers
    // ever see them, silently losing most autoconfiguration classes — including the one that registers the
    // embedded Tomcat server, so the REST server never actually starts. INCLUDE lets every duplicate reach
    // the transformers below, which then merge them instead of picking one arbitrarily.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    append("META-INF/spring.factories")
    append("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
    append("META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports")
}

tasks.shadowDistTar {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    dependsOn("syncFetcherPythonDeps")
    useJUnitPlatform {
        excludeTags("integration")
        excludeTags("openapi")
    }
    reports.html.required.set(true)
    reports.html.outputLocation.set(layout.buildDirectory.dir("testReportHtml"))
    reports.junitXml.required.set(false)
    finalizedBy(tasks.koverHtmlReport)
    finalizedBy(tasks.koverXmlReport)
    finalizedBy(tasks.koverPrintCoverage)
}

tasks.register<Test>("integrationTest") {
    dependsOn("syncFetcherPythonDeps")
    useJUnitPlatform {
        includeTags("integration")
    }
    description = "Runs all integration tests"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    reports.html.required.set(true)
    reports.html.outputLocation.set(layout.buildDirectory.dir("integrationTestReportHtml"))
    reports.junitXml.required.set(false)
}

tasks.register<Test>("createApiSpec") {
    dependsOn("syncFetcherPythonDeps")
    useJUnitPlatform {
        includeTags("openapi")
    }
    description = "Runs the OpenAPI spec generation"
    group = "build"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    reports.html.required.set(true)
    reports.junitXml.required.set(false)
}

/**
 * Runs `npm pkg set` in [dir] and fails the build if npm exits with a non-zero code.
 */
fun npmPkgSet(dir: File, vararg fields: String) {
    val exitCode = ProcessBuilder("npm", "pkg", "set", *fields)
        .directory(dir)
        .inheritIO()
        .start()
        .waitFor()
    if (exitCode != 0) {
        throw GradleException("`npm pkg set ${fields.joinToString(" ")}` in $dir failed with exit code $exitCode")
    }
}

// The committed OpenAPI spec is the single source of truth for the generated frontend client.
// `openApiGenerate` turns it into a publishable TypeScript npm package.
val committedApiSpec: RegularFile = layout.projectDirectory.file("api/openapi.json")
val tsRestClientDir: Provider<Directory> = layout.buildDirectory.dir("generated/ts-rest-client")

openApiGenerate {
    generatorName.set("typescript-fetch")
    inputSpec.set(committedApiSpec.asFile.absolutePath)
    outputDir.set(tsRestClientDir)
    validateSpec.set(true)
    gitUserId.set("SE-UUlm")
    gitRepoId.set("snowballr-backend")
    configOptions.set(
        mapOf(
            "npmName" to "@se-uulm/snowballr-api-client",
            "npmVersion" to project.version.toString(),
            "licenseName" to "GPL-3.0-or-later",
            "supportsES6" to "true",
            "withInterfaces" to "true",
            "enumPropertyNaming" to "UPPERCASE",
        ),
    )
}

tasks.named("openApiGenerate") {
    // Generate only from a spec that has been verified to match the code.
    dependsOn("createApiSpec")
    description = "Generates the TypeScript client from the committed OpenAPI spec"
    group = "build"
    inputs.file(committedApiSpec)
}

tasks.register<Exec>("buildTsRestClient") {
    dependsOn("openApiGenerate")
    description = "Installs npm dependencies for the generated TypeScript REST client and builds it"
    group = "publishing"
    workingDir = tsRestClientDir.get().asFile
    commandLine("npm", "install")

    // Copy the license and changelog to the package to make it ready for publishing
    doFirst {
        copy {
            from(".")
            into(tsRestClientDir.get())
            include("LICENSE")
            include("CHANGELOG.md")
        }

        // Restrict the published package contents to the compiled output and docs (source code is omitted).
        npmPkgSet(
            tsRestClientDir.get().asFile,
            "description=API client for the SnowballR Web Application",
            "author=SnowballR",
            "files[0]=dist",
            "files[1]=README.md",
            "files[2]=LICENSE",
            "files[3]=CHANGELOG.md",
            "repository[url]=git+https://github.com/SE-UUlm/snowballr-backend.git"
        )
    }
}

val tsGrpcClientDir: Provider<Directory> = layout.buildDirectory.dir("generated/ts-grpc-client")

tasks.register<Exec>("buildTsGrpcClient") {
    description = "Installs npm dependencies for the generated TypeScript gRPC client and builds it"
    group = "publishing"
    workingDir = tsGrpcClientDir.get().asFile
    commandLine("npm", "install")
    mkdir("${tsGrpcClientDir.get()}/dist")

    // Copy all required files to the package to make it ready for publishing
    doFirst {
        copy {
            into(tsGrpcClientDir.get())

            from("api/grpc") {
                include("package.json")
                include("proto/*.proto")
                include("README.md")
            }

            from(".") {
                include("LICENSE")
                include("CHANGELOG.md")
            }
        }

        npmPkgSet(
            tsGrpcClientDir.get().asFile,
            "version=${project.version}"
        )
    }
}

kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.add("integrationTest")
            disabledForTestTasks.add("createApiSpec")
        }
    }

    reports {
        filters {
            excludes {
                packages(
                    "snowballr", // generated grpc server
                    "se.uulm.snowballr.backend.db", // production database
                    "se.uulm.snowballr.backend.env", // environment variables
                    "se.uulm.snowballr.backend.grpc", // grpc server implementation
                    "se.uulm.snowballr.backend.scheduler", // job scheduler
                    "se.uulm.snowballr.backend.rest", // REST server implementation
                )
                classes(
                    "se.uulm.snowballr.backend.MainKt", // main entry point
                )
            }
        }

        verify {
            rule {
                disabled.set(false)
                groupBy.set(GroupingEntityType.APPLICATION)

                bound {
                    minValue.set(1)
                    maxValue.set(99)
                    coverageUnits.set(CoverageUnit.INSTRUCTION)
                    aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
                }

                minBound(1)
                maxBound(99)
            }
        }

        total {
            xml {
                onCheck.set(false)
                xmlFile.set(layout.buildDirectory.file("coverageXml/report.xml"))
            }
            html {
                onCheck.set(false)
                htmlDir.set(layout.buildDirectory.dir("coverageHtml"))
            }
            verify {
                onCheck.set(true)
            }
            log {
                onCheck.set(true)
                header.set(null as String?)
                format.set("<entity> instruction coverage: <value>%")
                groupBy.set(GroupingEntityType.APPLICATION)
                coverageUnits.set(CoverageUnit.INSTRUCTION)
                aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
            }
        }
    }
}

detekt {
    config.from("detekt.yml")
}

// detekt must run with the Kotlin version it was compiled with, not the project's (or Spring Boot's managed) version
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(dev.detekt.gradle.plugin.getSupportedKotlinVersion())
        }
    }
}

// https://github.com/grpc/grpc-kotlin/tree/master/compiler
// Note: since Spring Boot 4.1.1, the Spring Boot Gradle plugin (ProtobufPluginAction) no longer
// registers a "grpc" (Java) plugin locator itself. It only reacts to one created by the build: it
// sets an unversioned artifact and adds the plugin to all generateProto tasks. Its own version
// alignment for that artifact relies on resolving the runtimeClasspath configuration from inside
// another configuration's dependency resolution, which comes back empty in this project, leaving
// the artifact version unset. Our configure action runs after Spring Boot's, so pin the version here.
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.kotlin.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.asProvider().get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("api/grpc/proto")
            include("*.proto")
        }
    }
}

// Alias for regular detekt lint check
tasks.register<Detekt>("lint") {
    group = "verification"
    description = "Runs Detekt linter"

    config.setFrom(files("detekt.yml"))
    autoCorrect = false
}

// Custom format task as alias for "detekt --auto-correct"
tasks.register<Detekt>("format") {
    group = "verification"
    description = "Runs Detekt with the auto-correct flag to format the code."

    config.setFrom(files("detekt.yml"))
    autoCorrect = true
}

tasks.withType<Detekt>().configureEach {
    setSource(files("src"))
    include("**/*.kt", "**/*.kts")
    exclude {
        it.file.path.contains("build")
    }
    jvmTarget = "22"
    classpath = sourceSets["main"].runtimeClasspath
    baseline.set(file("$rootDir/detekt-baseline.xml"))
    parallel = true
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    setSource(files("src"))
    include("**/*.kt", "**/*.kts")
    exclude {
        it.file.path.contains("build")
    }
    jvmTarget = "22"
    classpath = sourceSets["main"].runtimeClasspath
    parallel = true
}

tasks.named<JavaExec>("run") {
    dependsOn("syncFetcherPythonDeps")
}

tasks.withType<DependencyUpdatesTask> {
    fun isNonStable(version: String): Boolean {
        // Major, Minor, Patch, and optional build version
        val isSemVer = "^\\d+\\.\\d+\\.\\d+(\\.\\d+){0,1}".toRegex().matches(version)

        return isSemVer.not()
    }

    // Only allow non-stable candidates for dependencies that are already on a non-stable version (e.g. detekt)
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}
