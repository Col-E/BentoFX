/*******************************************************************************
This is an unpublished work of SAIC.
Copyright (c) 2025 SAIC. All Rights Reserved.
 ******************************************************************************/

plugins {

    id("bento.project.project-convention")
    id("bento.project.application-convention")
    alias(libs.plugins.javafx.gradlePlugin)
}

description = "BentoFX Basic Demo"

application {
    applicationName = description ?: name
    mainModule = "bento.fx.demo.basic"
    mainClass = "software.coley.boxfx.demo.basic.Runner"
    applicationDefaultJvmArgs += listOf(
        "-Xms256m",
        "-Xmx1024m",
    )
}

javafx {
    modules = listOf(
        "javafx.controls",
    )
}

dependencies {

    implementation(projects.core)

    implementation(libs.javafx.base)
    implementation(libs.javafx.controls)
    implementation(libs.javafx.graphics)

    compileOnly(libs.jakarta.annotation)
}
