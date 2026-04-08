/*******************************************************************************
This is an unpublished work of SAIC.
Copyright (c) 2019 SAIC. All Rights Reserved.
 ******************************************************************************/

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
