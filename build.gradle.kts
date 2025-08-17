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
    application
}

group = "se.uulm.snowballr.backend"
version = "0.1.0"

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
    testImplementation(libs.koin.test)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    testImplementation(libs.testcontainers)

    implementation(libs.password4j)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.nanoid)

    implementation(libs.dotenv.kotlin)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertj.core)
    testImplementation(libs.assertj.arrow.core)
    testImplementation(libs.mockk)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform)

    testImplementation(libs.archunit)
    testImplementation(libs.archunit.junit5)

    implementation(libs.arrow.core)

    implementation(libs.grpc.kotlin)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.services)
    implementation(libs.protobuf.kotlin)
    implementation(libs.protobuf.java.util)

    runtimeOnly(libs.grpc.netty)

    detektPlugins(libs.detekt.formatting)

    implementation(libs.simple.java.mail)
    testImplementation(libs.greenmail.junit5)
    implementation(libs.handlebars)
    implementation(libs.kotlinx.serialization.json)
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveClassifier.set("") // omit the "all" suffix
}

tasks.test {
    useJUnitPlatform()
    reports.html.required.set(true)
    reports.html.outputLocation.set(layout.buildDirectory.dir("testReportHtml"))
    reports.junitXml.required.set(false)
    finalizedBy(tasks.koverHtmlReport)
    finalizedBy(tasks.koverXmlReport)
}

tasks.koverHtmlReport {
    dependsOn(tasks.test)
}

tasks.koverXmlReport {
    dependsOn(tasks.test)
}

kover {
    reports {
        filters {
            excludes {
                packages(
                    "snowballr", // generated grpc server
                    "se.uulm.snowballr.backend.db", // production database
                    "se.uulm.snowballr.backend.env", // environment variables
                    "se.uulm.snowballr.backend.grpc", // grpc server implementation
                    "se.uulm.snowballr.backend.model", // model classes, no logic
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
                format.set("<entity> line coverage: <value>%")
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
            srcDir("api/proto")
            include("*.proto")
        }
    }
}

// Alias for regular detekt lint check
tasks.register<io.gitlab.arturbosch.detekt.Detekt>("lint") {
    group = "verification"
    description = "Runs Detekt linter"

    config.setFrom(files("detekt.yml"))
    autoCorrect = false
}

// Custom format task as alias for "detekt --auto-correct"
tasks.register<io.gitlab.arturbosch.detekt.Detekt>("format") {
    group = "verification"
    description = "Runs Detekt with the auto-correct flag to format the code."

    config.setFrom(files("detekt.yml"))
    autoCorrect = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    setSource(files("src"))
    include("**/*.kt", "**/*.kts")
    exclude {
        it.file.path.contains("build")
    }
    jvmTarget = "1.8"
    classpath = sourceSets["main"].runtimeClasspath
    baseline.set(file("$rootDir/detekt-baseline.xml"))
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    setSource(files("src"))
    include("**/*.kt", "**/*.kts")
    exclude {
        it.file.path.contains("build")
    }
    jvmTarget = "1.8"
    classpath = sourceSets["main"].runtimeClasspath
    parallel = true
}
