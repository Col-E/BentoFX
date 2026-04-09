/*******************************************************************************
This is an unpublished work of SAIC.
Copyright (c) 2026 SAIC. All Rights Reserved.
 ******************************************************************************/

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        // Search ~/.m2
        mavenLocal()
        mavenCentral()
    }
}

// Fix the project name so it remains constant regardless of checkout directory.
// Used to qualify implicit JPMS module names, to align IntelliJ module names
// on project import, for the IntelliJ project name, and in the default
// configuration values of various Gradle plugins.
rootProject.name = "bento-fx"

// Feature flag implementing strictness during stabilization of configuration
// caching. Gradle documentation recommends enabling it to prepare for flag
// removal and making the linked features the default.  See also
// https://docs.gradle.org/current/userguide/configuration_cache.html
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

// Feature flag enabling type-safe project accessors.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("platform")

include(":core")

include(":demos:basic-demo")
