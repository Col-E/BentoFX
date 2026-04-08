/*******************************************************************************
This is an unpublished work of SAIC.
Copyright (c) 2019 SAIC. All Rights Reserved.
 ******************************************************************************/

plugins {
    `java-library`
}

// These properties are defined in gradle.properties
group = property("group") as String
version = property("version") as String

// Version catalog type-safe accessors not available in
// precompiled script plugins:
// https://github.com/gradle/gradle/issues/15383.
// Using version catalog API instead.
val versionCatalog: VersionCatalog = versionCatalogs.named("libs")

// Get the Java JDK version specified in libs.versions.toml
val jdkVersionName = "java-jdk"
val jdkVersion: String = versionCatalog.findVersion(jdkVersionName).get().requiredVersion

// Accommodate all the different ways Gradle and its plugins take the Java
// version...
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(jdkVersion)
val javaMajorVersionAsInt: Int = javaLanguageVersion.asInt()

dependencies {

    api(platform(project(":platform")))
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {

    register<DependencyReportTask>("allDependencies").configure {
        description = "Displays all dependencies declared in all subprojects."
        group = "help"
    }

    withType<Jar>().configureEach {

        // Name JARs using full project name, based on the project path.
        // Otherwise, we might get JARs with the same names that will collide
        // when aggregated by the installer task.
        val jarBaseName =
            project.path
                // Delete the leading ':'
                .substring(1)
                // Replace the remaining ':' with '.'
                .replace(':', '.')

        archiveBaseName.set(jarBaseName)
    }

    withType<JavaCompile>().configureEach {

        with(options) {
            // The character encoding to be used when reading source files into
            // the Java compiler. Defaults to null, in which case the platform
            // default encoding will be used. This is separable from the value
            // set in gradle.properties, which (currently) specifies encoding as
            // UTF-8. UTF-8 is a multibyte encoding that can represent any
            // Unicode character. ISO 8859-1 is a single-byte encoding that can
            // represent the first 256 Unicode characters. Both encode ASCII
            // exactly the same way.
            encoding = "UTF-8"

            release = javaMajorVersionAsInt

            // Required for Spring 6.1+:
            // https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.1-Release-Notes#parameter-name-retention
            compilerArgs.add("-parameters")

            isFork = true
            forkOptions.memoryMaximumSize = "1g"
        }
    }

    named<Javadoc>("javadoc").configure {
        (options as? StandardJavadocDocletOptions)
            ?.addStringOption("Xdoclint:none", "-quiet")
    }

    withType<JavaExec>().configureEach {

        javaLauncher = javaToolchains.launcherFor {
            languageVersion = javaLanguageVersion
        }
    }

    withType<Test>().configureEach {

        javaLauncher = javaToolchains.launcherFor {
            languageVersion = javaLanguageVersion
        }
    }
}
