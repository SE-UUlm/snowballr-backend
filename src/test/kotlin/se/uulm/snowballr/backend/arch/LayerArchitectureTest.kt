// Suppress the 'FunctionName' rule because it cannot detect that this is a test file.
// Suppress the 'ForbiddenMethodCall' rule because we can use println here.
@file:Suppress("FunctionName", "ForbiddenMethodCall")

package se.uulm.snowballr.backend.arch

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchTests
import com.tngtech.archunit.lang.conditions.ArchConditions.haveNameMatching
import com.tngtech.archunit.lang.conditions.ArchConditions.haveSimpleName
import com.tngtech.archunit.lang.conditions.ArchConditions.haveSimpleNameEndingWith
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import com.tngtech.archunit.library.metrics.ArchitectureMetrics
import com.tngtech.archunit.library.metrics.MetricsComponents

private const val BASE_PACKAGE = "se.uulm.snowballr.backend"

@AnalyzeClasses(packages = [BASE_PACKAGE], importOptions = [ImportOption.DoNotIncludeTests::class])
@Suppress("UNUSED")
class LayerArchitectureTest {
    @ArchTest
    val structureRules: ArchTests = ArchTests.`in`(StructureRules::class.java)

    @ArchTest
    val namingConventions: ArchTests = ArchTests.`in`(NamingConventions::class.java)

    @ArchTest
    val metrics: ArchTests = ArchTests.`in`(Metrics::class.java)
}

private class StructureRules {
    @ArchTest
    fun `When the layer architecture is violated, then this test should fail (all deps)`(classes: JavaClasses) {
        layeredArchitecture()
            .consideringAllDependencies()
            // Main layer: Main.kt and Module.kt
            .layer("Main")
            .definedBy(BASE_PACKAGE)
            // Input validation layer
            .layer("Input Validation")
            .definedBy("$BASE_PACKAGE.validation..")
            // gRPC Server layer including the interceptors
            .layer("gRPC Server")
            .definedBy("$BASE_PACKAGE.grpc..")
            // Service layer
            .layer("Service")
            .definedBy("$BASE_PACKAGE.service..")
            // Access Checkers
            .layer("Access")
            .definedBy("$BASE_PACKAGE.access..")
            // Repository layer
            .layer("Repository")
            .definedBy("$BASE_PACKAGE.repository..")
            // Table layer
            .layer("Table")
            .definedBy("$BASE_PACKAGE.table..")
            // DB layer
            .layer("DB")
            .definedBy("$BASE_PACKAGE.db..")
            // Scheduler / Cron Jobs
            .layer("Scheduler")
            .definedBy("$BASE_PACKAGE.scheduler..")
            // Fetcher
            .layer("Fetcher")
            .definedBy("$BASE_PACKAGE.fetcher..")
            // Checks
            .whereLayer("Input Validation")
            .mayOnlyBeAccessedByLayers("gRPC Server")
            .whereLayer("gRPC Server")
            .mayOnlyBeAccessedByLayers("Main")
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("gRPC Server", "Main")
            .whereLayer("Access")
            .mayOnlyBeAccessedByLayers("Service", "Main")
            .whereLayer("Repository")
            .mayOnlyBeAccessedByLayers("Service", "Access", "Scheduler", "Main", "Fetcher")
            .whereLayer("Table")
            .mayOnlyBeAccessedByLayers("Repository", "DB")
            .whereLayer("DB")
            .mayOnlyBeAccessedByLayers("Main", "Repository")
            .whereLayer("Scheduler")
            .mayOnlyBeAccessedByLayers("gRPC Server", "Main")
            .whereLayer("Fetcher")
            .mayOnlyBeAccessedByLayers("Main", "Service")
            .check(classes)
    }

    @ArchTest
    fun `When the layer architecture is violated, then this test should fail (only layer deps)`(classes: JavaClasses) {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            // Main layer: Main.kt and Module.kt
            .layer("Main")
            .definedBy(BASE_PACKAGE)
            // Input validation layer
            .layer("Input Validation")
            .definedBy("$BASE_PACKAGE.validation..")
            // gRPC Server layer including the interceptors
            .layer("gRPC Server")
            .definedBy("$BASE_PACKAGE.grpc..")
            // Service layer
            .layer("Service")
            .definedBy("$BASE_PACKAGE.service..")
            // Access Checkers
            .layer("Access")
            .definedBy("$BASE_PACKAGE.access..")
            // Repository layer
            .layer("Repository")
            .definedBy("$BASE_PACKAGE.repository..")
            // Table layer
            .layer("Table")
            .definedBy("$BASE_PACKAGE.table..")
            // DB layer
            .layer("DB")
            .definedBy("$BASE_PACKAGE.db..")
            // Scheduler / Cron Jobs
            .layer("Scheduler")
            .definedBy("$BASE_PACKAGE.scheduler..")
            // Fetcher
            .layer("Fetcher")
            .definedBy("$BASE_PACKAGE.fetcher..")
            // Checks
            .whereLayer("Main")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Main")
            .mayOnlyAccessLayers("gRPC Server", "Service", "Access", "Repository", "DB", "Scheduler", "Fetcher")
            .whereLayer("Input Validation")
            .mayNotAccessAnyLayer()
            .whereLayer("gRPC Server")
            .mayOnlyAccessLayers("Service", "Input Validation", "Scheduler")
            .whereLayer("Service")
            .mayOnlyAccessLayers("Repository", "Access", "Fetcher")
            .whereLayer("Access")
            .mayOnlyAccessLayers("Repository")
            .whereLayer("Repository")
            .mayOnlyAccessLayers("Table", "DB")
            .whereLayer("Table")
            .mayNotAccessAnyLayer()
            .whereLayer("DB")
            .mayOnlyAccessLayers("Table")
            .whereLayer("Scheduler")
            .mayOnlyAccessLayers("Repository")
            .whereLayer("Fetcher")
            .mayOnlyAccessLayers("Repository")
            .check(classes)
    }

    @ArchTest
    fun `When the project's classes contains cycles, then this test should fail`(classes: JavaClasses) {
        slices()
            .matching("$BASE_PACKAGE.(*)..")
            .should()
            .beFreeOfCycles()
            .check(classes)
    }
}

private class NamingConventions {
    @ArchTest
    fun `When a class is in the validation package, then it should have the 'Validator' suffix`(classes: JavaClasses) {
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.validation..")
            .should()
            .haveSimpleNameEndingWith("Validator")
            .orShould(haveSimpleNameEndingWith("ValidatorKt")) // kotlin class
            .orShould(haveSimpleName("ValidationHelperKt")) // exception
            .because("All validators should have the 'Validator' suffix")
            .check(classes)
    }

    @ArchTest
    fun `When a class is in the service package, then it should have the 'Service' suffix`(classes: JavaClasses) {
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.service..")
            .should()
            .haveNameMatching(".*Service.*")
            .because("All services should have the 'Service' suffix")
            .check(classes)
    }

    @ArchTest
    fun `When a class is in the access package, then it should have the 'AccessRule' or 'AccessChecker' suffix`(
        classes: JavaClasses,
    ) {
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.access..")
            .should()
            .haveNameMatching(".*AccessChecker.*")
            .orShould(haveNameMatching(".*AccessRule.*"))
            .because("All access checkers should have the 'AccessChecker' or 'AccessRule' suffix")
            .check(classes)

        // Only access rules
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.access.rules..")
            .should()
            .haveNameMatching(".*AccessRule.*")
            .because("All access rules should have the 'AccessRule' suffix")
            .check(classes)
    }

    @ArchTest
    fun `When a class is in the repository package, then it should have the 'TableRepo' suffix`(classes: JavaClasses) {
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.repository..")
            .should()
            .haveNameMatching(".*TableRepo.*")
            .orShould(haveNameMatching(".*RepoHelperKt.*")) // exception
            .orShould(haveNameMatching(".*RepoResultHelperKt.*")) // exception
            .orShould(haveSimpleName("SqlStateHelperKt")) // exception
            .because("All repositories should have the 'TableRepo' suffix")
            .check(classes)
    }

    @ArchTest
    fun `When a class is in the table package, then it should have the 'Table' suffix`(classes: JavaClasses) {
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.table..")
            .should()
            .haveSimpleNameEndingWith("Table")
            .orShould(haveSimpleNameEndingWith("TableKt")) // kotlin class
            .orShould(haveSimpleNameEndingWith("ColumnHelperKt")) // exception
            .orShould(haveSimpleName("TableHelperKt")) // exception
            .orShould(haveSimpleNameEndingWith("ObfuscatedTextColumnType")) // exception
            .orShould(haveSimpleNameEndingWith("RedactedBinaryColumnType")) // exception
            .orShould(haveSimpleName("HStoreColumnType")) // exception
            .orShould(haveNameMatching(".*inlined\\\$json\\\$.*")) // exception
            .because("All tables should have the 'Table' suffix")
            .check(classes)
    }
}

private class Metrics {
    @ArchTest
    fun `Print Lakos Metrics`(classes: JavaClasses) {
        val packages = classes.getPackage(BASE_PACKAGE).subpackages
        val components = MetricsComponents.fromPackages(packages)
        val metrics = ArchitectureMetrics.lakosMetrics(components)

        println("== Lakos Metrics ==")
        println("CCD:  " + metrics.cumulativeComponentDependency)
        println("ACD:  " + metrics.averageComponentDependency)
        println("RACD: " + metrics.relativeAverageComponentDependency)
        println("NCCD: " + metrics.normalizedCumulativeComponentDependency)
    }

    @ArchTest
    fun `Print Dependency Metrics`(classes: JavaClasses) {
        val packages = classes.getPackage(BASE_PACKAGE).subpackages
        val components = MetricsComponents.fromPackages(packages)
        val metrics = ArchitectureMetrics.componentDependencyMetrics(components)

        println("== Component Dependency Metrics ==")
        val identifiers = arrayOf("repository", "service", "validation")
        for (identifier in identifiers) {
            val finalIdentifier = "$BASE_PACKAGE.$identifier"
            println("=== $finalIdentifier ===")
            println("Ce: " + metrics.getEfferentCoupling(finalIdentifier))
            println("Ca: " + metrics.getAfferentCoupling(finalIdentifier))
            println("I:  " + metrics.getInstability(finalIdentifier))
            println("A:  " + metrics.getAbstractness(finalIdentifier))
            println("D:  " + metrics.getNormalizedDistanceFromMainSequence(finalIdentifier))
        }
    }

    @ArchTest
    fun `Print Dowalil Visibility Metrics`(classes: JavaClasses) {
        val packages = classes.getPackage(BASE_PACKAGE).subpackages
        val components = MetricsComponents.fromPackages(packages)
        val metrics = ArchitectureMetrics.visibilityMetrics(components)

        println("== Dowalil Visibility Metrics ==")
        println("ARV: " + metrics.averageRelativeVisibility)
        println("GRV: " + metrics.globalRelativeVisibility)

        val identifiers = arrayOf("repository", "service", "validation")
        val maxLength = identifiers.maxOf { it.length } + "$BASE_PACKAGE.".length
        for (identifier in identifiers) {
            val finalIdentifier = "$BASE_PACKAGE.$identifier"
            val displayId = "$finalIdentifier:".padEnd(maxLength)
            println(
                "RV of $displayId " + metrics.getRelativeVisibility(finalIdentifier),
            )
        }
    }
}
