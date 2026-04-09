plugins {
    id("java-platform")
}

// See also Gradle Version Catalog at '<root>\gradle\libs.versions.toml'.

javaPlatform {
    allowDependencies()
}

dependencies {

    /*
     * BOMs
     * Ordering matters! Declaring a BOM before subsequent platform declarations
     * will cause third-party dependencies declared in the BOM to be overridden
     * by those platform declarations.
     */

    api(platform(libs.junit.bom)) {
        version {
            strictly(libs.versions.junit.get())
        }
    }

    constraints {

        /*
         * Bundles
         */

        api(libs.bundles.javafx)

        /*
         * Individual Dependencies
         */
    }
}
