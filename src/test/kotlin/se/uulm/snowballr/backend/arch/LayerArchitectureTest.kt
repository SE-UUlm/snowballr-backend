// Suppress the 'FunctionName' rule because it cannot detect that this is a test file.
// Suppress the 'ForbiddenMethodCall' rule because we can use println here.
@file:Suppress("FunctionName", "ForbiddenMethodCall")

package se.uulm.snowballr.backend.arch

import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchTests
import com.tngtech.archunit.lang.conditions.ArchConditions.haveNameMatching
import com.tngtech.archunit.lang.conditions.ArchConditions.haveSimpleName
import com.tngtech.archunit.lang.conditions.ArchConditions.haveSimpleNameEndingWith
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures
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
    companion object {
        // Layer names
        private const val MAIN = "Main"
        private const val VALIDATION = "Input Validation"
        private const val GRPC = "gRPC Server"
        private const val SERVICE = "Service"
        private const val ACCESS = "Access"
        private const val REPO = "Repository"
        private const val TABLE = "Table"
        private const val DB = "DB"
        private const val SCHEDULER = "Scheduler"
        private const val FETCHER = "Fetcher"
    }

    private fun Architectures.LayeredArchitecture.snowballRLayers() = this
        // Main layer: Main.kt and Module.kt
        .layer(MAIN)
        .definedBy(BASE_PACKAGE)
        // Input validation layer
        .layer(VALIDATION)
        .definedBy("$BASE_PACKAGE.validation..")
        // gRPC Server layer including the interceptors
        .layer(GRPC)
        .definedBy("$BASE_PACKAGE.grpc..")
        // Service layer
        .layer(SERVICE)
        .definedBy("$BASE_PACKAGE.service..")
        // Access Checkers
        .layer(ACCESS)
        .definedBy("$BASE_PACKAGE.access..")
        // Repository layer
        .layer(REPO)
        .definedBy("$BASE_PACKAGE.repository..")
        // Table layer
        .layer(TABLE)
        .definedBy("$BASE_PACKAGE.table..")
        // DB layer
        .layer(DB)
        .definedBy("$BASE_PACKAGE.db..")
        // Scheduler / Cron Jobs
        .layer(SCHEDULER)
        .definedBy("$BASE_PACKAGE.scheduler..")
        // Fetcher
        .layer(FETCHER)
        .definedBy("$BASE_PACKAGE.fetcher..")

    @ArchTest
    fun `When the layer architecture is violated, then this test should fail (all deps)`(classes: JavaClasses) {
        layeredArchitecture()
            .consideringAllDependencies()
            .snowballRLayers()
            // Checks
            .whereLayer(VALIDATION)
            .mayOnlyBeAccessedByLayers(GRPC)
            .whereLayer(GRPC)
            .mayOnlyBeAccessedByLayers(MAIN)
            .whereLayer(SERVICE)
            .mayOnlyBeAccessedByLayers(GRPC, MAIN)
            .whereLayer(ACCESS)
            .mayOnlyBeAccessedByLayers(SERVICE, MAIN)
            .whereLayer(REPO)
            .mayOnlyBeAccessedByLayers(SERVICE, ACCESS, SCHEDULER, MAIN, FETCHER)
            .whereLayer(TABLE)
            .mayOnlyBeAccessedByLayers(REPO, DB)
            .whereLayer(DB)
            .mayOnlyBeAccessedByLayers(MAIN, REPO)
            .whereLayer(SCHEDULER)
            .mayOnlyBeAccessedByLayers(GRPC, MAIN)
            .whereLayer(FETCHER)
            .mayOnlyBeAccessedByLayers(MAIN, SERVICE)
            .check(classes)
    }

    @ArchTest
    fun `When the layer architecture is violated, then this test should fail (only layer deps)`(classes: JavaClasses) {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .snowballRLayers()
            // Checks
            .whereLayer(MAIN)
            .mayNotBeAccessedByAnyLayer()
            .whereLayer(MAIN)
            .mayOnlyAccessLayers(GRPC, SERVICE, ACCESS, REPO, DB, SCHEDULER, FETCHER)
            .whereLayer(VALIDATION)
            .mayNotAccessAnyLayer()
            .whereLayer(GRPC)
            .mayOnlyAccessLayers(SERVICE, VALIDATION, SCHEDULER)
            .whereLayer(SERVICE)
            .mayOnlyAccessLayers(REPO, ACCESS, FETCHER)
            .whereLayer(ACCESS)
            .mayOnlyAccessLayers(REPO)
            .whereLayer(REPO)
            .mayOnlyAccessLayers(TABLE, DB)
            .whereLayer(TABLE)
            .mayNotAccessAnyLayer()
            .whereLayer(DB)
            .mayOnlyAccessLayers(TABLE)
            .whereLayer(SCHEDULER)
            .mayOnlyAccessLayers(REPO)
            .whereLayer(FETCHER)
            .mayOnlyAccessLayers(REPO)
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
            .orShould(haveNameMatching($$".*inlined\\$zipOrAccumulate\\$.*")) // inlined function exception
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
        // Only access checkers
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.access..")
            .and(not(resideInAPackage("$BASE_PACKAGE.access.rules..")))
            .should()
            .haveNameMatching(".*AccessChecker.*")
            .because("All access checkers should have the 'AccessChecker' suffix")
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
        // Only tables and helpers, not column types
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.table..")
            .and(not(resideInAPackage("$BASE_PACKAGE.table.columntypes")))
            .should()
            .haveSimpleNameEndingWith("Table")
            .orShould(haveSimpleNameEndingWith("TableKt")) // kotlin class
            .orShould(haveSimpleNameEndingWith("ColumnHelperKt")) // exception
            .orShould(haveSimpleName("TableHelperKt")) // exception
            .orShould(haveNameMatching($$".*inlined\\$json\\$.*")) // inlined function exception
            .orShould(haveNameMatching($$".*inlined\\$obfuscatedJson\\$.*")) // inlined function exception
            .because("All tables should have the 'Table' suffix")
            .check(classes)

        // Only column types
        classes()
            .that()
            .resideInAPackage("$BASE_PACKAGE.table.columntypes..")
            .should()
            .haveNameMatching(".*ColumnType.*")
            .because("All column types should have the 'ColumnType' suffix")
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
