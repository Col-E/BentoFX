plugins {
    // Not using `id("kotlin-dsl")` syntax per
    // https://github.com/gradle/gradle/issues/23884
    `kotlin-dsl`
}

description = "Used to apply plugins to projects and to configure " +
        "dependencies and tasks. Runs during project configuration, after " +
        "settings."

dependencies {

    implementation(gradleApi())
    implementation(libs.jreleaser.gradlePlugin.dependency)
}
