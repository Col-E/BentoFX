/*******************************************************************************
This is an unpublished work of SAIC.
Copyright (c) 2026 SAIC. All Rights Reserved.
 ******************************************************************************/

import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.jreleaser.gradle.plugin.tasks.*

plugins {
    // Apply the base plugin to add `clean` to the root project.
    id("base")
    alias(libs.plugins.jreleaser.gradlePlugin)
}

/**
 * The directory to which JReleaser artifacts are staged.
 */
val stagingDir: Provider<Directory> =
    layout.buildDirectory.dir("staging-deploy")

tasks {

    // Configuration cache incompatibility message
    val jreleaserConfigCacheIncompatibilityReason =
        "As of version 1.23.0, the JReleaser Gradle plugin does not support " +
                "configuration cache: " +
                "(https://github.com/jreleaser/jreleaser/issues/1992).\n" +
                "Run `jreleaser[TaskName]` tasks using:\n" +
                "`./gradlew jreleaser[TaskName] --no-configuration-cache'"


    named<JReleaserAssembleTask>("jreleaserAssemble").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        // Only depend on modules that actually build Java artifacts
        dependsOn(subprojects.mapNotNull { subproject ->
            subproject.tasks.matching {
                it.name == "assemble"
            }.takeIf {
                it.isNotEmpty()
            }?.let {
                subproject.tasks.named("assemble")
            }
        })
    }

    // Publish all module publications to the shared staging repo
    val stageAllPublications: TaskProvider<Task> by registering {
        group = "release"
        description = "Stages all subproject Maven publications into build/staging-deploy"

        dependsOn(
            subprojects.mapNotNull { subproject ->
                // Only projects that applied maven-publish + have the publication/repo will have this task.
                subproject.tasks.names
                    .firstOrNull { it == "publishMavenPublicationToStagingRepository" }
                    ?.let { subproject.tasks.named(it) }
            }
        )
    }

    named<JReleaserChecksumTask>("jreleaserChecksum").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        dependsOn(stageAllPublications)
    }

    named<JReleaserConfigTask>("jreleaserConfig").configure {
        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )
    }

    named<JReleaserDeployTask>("jreleaserDeploy").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        dependsOn(stageAllPublications)
    }

    named<JReleaserFullReleaseTask>("jreleaserFullRelease").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        dependsOn(subprojects.mapNotNull { subproject ->
            subproject.tasks.matching {
                it.name == "build"
            }.takeIf {
                it.isNotEmpty()
            }?.let {
                subproject.tasks.named("build")
            }
        })
    }

    named<JReleaserReleaseTask>("jreleaserRelease").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        dependsOn(stageAllPublications)
    }

    named<JReleaserSignTask>("jreleaserSign").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        dependsOn(stageAllPublications)
    }

    val stageMavenCentral: TaskProvider<Sync> by registering(Sync::class) {

        group = "release"
        description = "Stages all subproject publications into build/staging-deploy"

        into(layout.buildDirectory.dir("staging-deploy"))

        subprojects.forEach { subproject ->
            // This path exists after publishing tasks run. We’ll depend on publishToMavenLocal
            // OR better: depend on each module's "publishMavenPublicationToMavenLocal" equivalent.
            // But task names vary; so we stage from known build outputs when available.

            // If you *do* use maven-publish, the publication metadata/jars are in build/libs + generated POM.
            // We'll stage jars + pom from build/publications/maven (generated metadata).
            from(subproject.layout.buildDirectory.dir("publications/maven")) {
                into("${subproject.name}/publications/maven")
            }
            from(subproject.layout.buildDirectory.dir("libs")) {
                into("${subproject.name}/libs")
            }
        }
    }

    named<JReleaserUploadTask>("jreleaserUpload").configure {

        notCompatibleWithConfigurationCache(
            jreleaserConfigCacheIncompatibilityReason
        )

        dependsOn(stageMavenCentral)
    }

    // Use this task to upgrade Gradle in order to keep
    // gradle-wrapper.properties in sync.
    named<Wrapper>("wrapper").configure {
        // Get the Gradle version specified in libs.versions.toml
        gradleVersion = libs.versions.gradle.get()
        // Using `ALL` instead of `BIN` to provide support for developers using
        // the Gradle scripts.
        distributionType = ALL
        networkTimeout = 60000
    }
}

jreleaser {

    // The release.github.token value must be configured using one of the
    // following:
    //   1. An environment variable `JRELEASER_GITHUB_TOKEN`
    //   2. A system property `jreleaser.github.token`
    //   3. A key/value pair in ${user.home}/.jreleaser/config.properties with a
    //      key named `JRELEASER_GITHUB_TOKEN`
    //   4. The Gradle DSL (not recommended for security reasons)

    gitRootSearch = true

    signing {
        pgp {
            setActive("RELEASE")
            armored = true
        }
    }

    files {
        setActive("RELEASE")
        glob {
            pattern = stagingDir.get().asFile.resolve("**/*.jar").path
        }
    }

    release {
        // TODO: This doesn't auto-publish github releases and the
        //  'distribution' block also isn't a viable alternative. Need to look
        //  into why it doesn't work. Probably related to the project's
        //  "alternative" artifact model...
        github {

            files = true
            artifacts = false
            checksums = true
            signatures = true
            tagName = project.version

            changelog {
                setFormatted("ALWAYS")
                preset = "conventional-commits"
                contributors {
                    format =
                        "- {{contributorName}}{{#contributorUsernameAsLink}} ({{.}}){{/contributorUsernameAsLink}}"
                }
            }
        }
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    setActive("RELEASE")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    applyMavenCentralRules = true
                    stagingRepository(stagingDir.get().asFile.path)
                }
            }
        }
    }
}
