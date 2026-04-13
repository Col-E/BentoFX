plugins {
	id("bento.project.project-convention")
	id("bento.release.publish-convention")
	alias(libs.plugins.javafx.gradlePlugin)
}

description = "A docking system for JavaFX"

dependencies {

	compileOnlyApi(libs.javafx.controls)

    compileOnly(libs.jakarta.annotation)
}
