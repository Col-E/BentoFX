import org.jreleaser.model.Active

plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.javafxplugin)
    alias(libs.plugins.jreleaser)
}

group = "software.coley"
version = "0.15.1"


dependencies {
    api(libs.jspecify)
    api(libs.javafx.controls)
    api(libs.javafx.graphics)

    compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")
    testCompileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // Test dependencies
    testImplementation(platform(libs.junit.jupiter))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        addBooleanOption("Xdoclint:none", true)
    }
}



publishing {
    repositories {
        mavenLocal()
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name = project.name
                description = "A docking system for JavaFX."
                url = "https://github.com/Col-E/BentoFX"
                inceptionYear = "2025"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://spdx.org/licenses/MIT.html"
                    }
                }
                developers {
                    developer {
                        id = "Col-E"
                        name = "Matt Coley"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/Col-E/BentoFX.git"
                    developerConnection = "scm:git:ssh://github.com/Col-E/BentoFX.git"
                    url = "https://github.com/Col-E/BentoFX"
                }
            }

        }
    }

}
jreleaser {
    signing {
        pgp {
            active = Active.RELEASE
            armored = true
        }
    }
    release {
        // TODO: This doesn't auto-publish GitHub releases and the 'distribution' block also isn't a viable alternative
        //  Need to look into why it doesn't work. Probably related to the project's "alternative" artifact model...
        github {
            tagName = project.version
            changelog {
                formatted = Active.ALWAYS
                preset = "conventional-commits"
                contributors {
                    format = "- {{contributorName}}{{#contributorUsernameAsLink}} ({{.}}){{/contributorUsernameAsLink}}"
                }
            }
        }
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = Active.RELEASE
                    url = "https://central.sonatype.com/api/v1/publisher"
                    applyMavenCentralRules = true
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}