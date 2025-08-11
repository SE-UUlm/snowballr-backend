pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    // Highly recommended, see https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:centralized-repository-declaration
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "snowballr-backend"
