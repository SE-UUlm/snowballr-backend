import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import de.undercouch.gradle.tasks.download.Download
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.shadow.jar)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.undercouch.download)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.ben.manes.versions)
    application
}

sonar {
    properties {
        property("sonar.projectKey", "snowballr-backend")
        property("sonar.projectName", "SnowballR Backend")
        property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("coverageXml/report.xml").get().asFile.path)
    }
}

group = "se.uulm.snowballr.backend"
version = "0.0.0"

// Snowballr API version to use for the proto files
val apiVersion = "0.14.0" // can be a tag (e.g., "1.2.3"), commit hash, or branch name like "main"
val escapedApiVersion = apiVersion.replace('/', '-')
val protoDir: Provider<Directory> = layout.buildDirectory.dir("snowballr-api/${escapedApiVersion}")

gitVersioning.apply {
    refs {
        tag("v(?<version>\\d+\\.\\d+\\.\\d+)") {
            version = $$"${ref.version}"
        }
    }

    // optional fallback configuration in case of no matching ref configuration
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

    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    runtimeOnly(libs.grpc.netty)

    testRuntimeOnly(libs.junit.platform)

    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(25)
}

val fetcherVenvDir = layout.projectDirectory.dir(".venv").asFile
val fetcherVenvPython: String = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    fetcherVenvDir.resolve("Scripts/python.exe").absolutePath
} else {
    fetcherVenvDir.resolve("bin/python3").absolutePath
}

tasks.register<Exec>("createFetcherVenv") {
    group = "application"
    description = "Creates the local Python virtual environment for fetcher plugins."
    workingDir = projectDir
    commandLine("uv", "venv", "--allow-existing", ".venv")
}

tasks.register<Exec>("syncFetcherPythonDeps") {
    group = "application"
    description = "Synchronizes Python fetcher dependencies from requirements.txt using uv."
    dependsOn("createFetcherVenv")
    workingDir = projectDir
    commandLine("uv", "pip", "install", "--python", fetcherVenvPython, "-r", "requirements.txt")
}

tasks.register("setupFetcherPython") {
    group = "application"
    description = "Prepares Python tooling required by fetcher plugins."
    dependsOn("syncFetcherPythonDeps")
}

tasks.shadowJar {
    archiveClassifier.set("") // omit the "all" suffix
}

tasks.shadowDistTar {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    dependsOn("syncFetcherPythonDeps")
    useJUnitPlatform {
        excludeTags("integration")
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

kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.add("integrationTest")
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

// https://github.com/grpc/grpc-kotlin/tree/master/compiler
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
                create("grpc")
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
            srcDir(protoDir)
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

tasks.register<Download>("downloadApiFiles") {
    group = "other"
    description = "Downloads the API definition files from the snowballr-api repository."

    val isTag = apiVersion.matches(Regex("""\d+.\d+.\d+"""))
    val isCommit = apiVersion.matches(Regex("""[a-f0-9]{40}"""))
    val isBranch = !isTag && !isCommit
    println("Using API version: $apiVersion (isTag: $isTag, isCommit: $isCommit, isBranch: $isBranch)")

    val zipUrl =
        if (isTag) "https://github.com/SE-UUlm/snowballr-api/archive/refs/tags/v${apiVersion}.zip"
        else if (isCommit) "https://github.com/SE-UUlm/snowballr-api/archive/${apiVersion}.zip"
        else "https://github.com/SE-UUlm/snowballr-api/archive/refs/heads/${apiVersion}.zip"

    val zipFile = layout.buildDirectory.file("snowballr-api-${escapedApiVersion}.zip").get().asFile
    val protoDirAsFile = protoDir.get().asFile

    // Declare inputs & outputs for caching
    inputs.property("apiVersion", apiVersion)
    outputs.dir(protoDir)
    outputs.cacheIf { !isBranch } // don't cache if apiVersion is a branch

    src(zipUrl)
    dest(zipFile)
    overwrite(isBranch) // don't re-download unless file missing or apiVersion is a branch

    doLast {
        // Only unzip if the directory is empty
        if (protoDirAsFile.list()?.isNotEmpty() == true && !isBranch) {
            println("API proto files already extracted - skipping unzip")
            return@doLast
        }

        println("Extracting $zipFile to $protoDirAsFile")

        fun unzipFile(zip: ZipFile, entry: ZipEntry) {
            // We only want the proto files
            if (!entry.name.startsWith("snowballr-api-${escapedApiVersion}/proto/")) return
            val entryName = entry.name.removePrefix("snowballr-api-${escapedApiVersion}/proto/")

            val outputFile = protoDirAsFile.resolve(entryName)
            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                outputFile.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                unzipFile(zip, entry)
            }
        }
    }
}

tasks.named("extractProto") {
    dependsOn("downloadApiFiles")
}

tasks.named("processResources") {
    dependsOn("downloadApiFiles")
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

    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
